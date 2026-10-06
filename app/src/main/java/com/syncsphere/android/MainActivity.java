package com.syncsphere.android;

import android.app.*;
import android.content.*;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import org.webrtc.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends Activity {
    private static final int CAPTURE_REQUEST=4101;
    private static final long CAPTURE_SERVICE_BIND_TIMEOUT_MS=10000;
    private EditText serverInput, sessionInput;
    private TextView status;
    private Button startButton;
    private PeerConnectionFactory factory;
    private PeerConnection peer;
    private VideoSource videoSource;
    private DataChannel dataChannel;
    private StompClient stomp;
    private final List<IceCandidate> pendingIceCandidates = new CopyOnWriteArrayList<>();
    private final Queue<IceCandidate> pendingLocalIceCandidates = new ConcurrentLinkedQueue<>();
    private volatile boolean remoteDescriptionSet;
    private volatile int sentIceCandidateCount;
    private volatile int receivedIceCandidateCount;
    private volatile int generatedIceCandidateCount;
    private volatile PeerConnection.IceConnectionState iceConnectionState =
            PeerConnection.IceConnectionState.NEW;
    private volatile PeerConnection.IceGatheringState iceGatheringState =
            PeerConnection.IceGatheringState.NEW;
    private volatile boolean localAnswerSet;
    private volatile boolean localAnswerHasIceCredentials;
    private String sessionId, deviceId;
    private MediaProjectionManager projectionManager;
    private ClipboardManager clipboard;
    private boolean suppressClipboard;
    private ScreenCaptureService captureService;
    private boolean captureServiceBound;
    private Intent capturePermissionData;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable captureServiceBindTimeout = () -> {
        if (captureServiceBound || capturePermissionData == null) return;
        capturePermissionData = null;
        status.setText("Screen capture service did not connect. Close and reopen SyncSphere, then try again.");
        stopService(new Intent(this,ScreenCaptureService.class));
        tearDownWebRTC();
    };

    private final ServiceConnection captureServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mainHandler.removeCallbacks(captureServiceBindTimeout);
            ScreenCaptureService.LocalBinder binder = (ScreenCaptureService.LocalBinder) service;
            captureService = binder.getService();
            captureServiceBound = true;
            if (capturePermissionData != null) {
                status.setText("Capture service ready. Preparing video…");
                Intent permissionData = capturePermissionData;
                capturePermissionData = null;
                continueScreenCapture(permissionData);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mainHandler.removeCallbacks(captureServiceBindTimeout);
            captureService = null;
            captureServiceBound = false;
            if (status != null) status.setText("Screen capture service disconnected.");
        }
    };

    @Override protected void onCreate(Bundle b){ super.onCreate(b); buildUi(); deviceId=UUID.randomUUID().toString(); }
    private void buildUi(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(32,32,32,32);
        TextView title=new TextView(this); title.setText("SyncSphere Android Remote"); title.setTextSize(24); box.addView(title);
        TextView help=new TextView(this); help.setText("Enter the Windows SyncSphere server address and the session code shown on the Windows Remote Access page."); box.addView(help);
        serverInput=new EditText(this); serverInput.setHint("ws://192.168.1.10:8080/ws"); serverInput.setText("ws://10.0.2.2:8080/ws"); box.addView(serverInput);
        sessionInput=new EditText(this); sessionInput.setHint("Session code"); box.addView(sessionInput);
        status=new TextView(this); status.setText("Disconnected"); box.addView(status);
        startButton=new Button(this); startButton.setText("Start screen sharing"); box.addView(startButton);
        Button accessibility=new Button(this); accessibility.setText("Enable remote touch permission"); box.addView(accessibility);
        setContentView(box);
        startButton.setOnClickListener(v->beginCapture());
        accessibility.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboard.addPrimaryClipChangedListener(()->{
            if(suppressClipboard || dataChannel==null || dataChannel.state()!=DataChannel.State.OPEN) return;
            try { String text=clipboard.getPrimaryClip().getItemAt(0).coerceToText(this).toString(); sendData(new JSONObject().put("type","CLIPBOARD").put("text",text)); } catch(Exception ignored){}
        });
    }
    private void beginCapture(){
        sessionId=sessionInput.getText().toString().trim().toUpperCase();
        if(sessionId.isEmpty()){sessionInput.setError("Enter the Windows session code");return;}
        projectionManager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(),CAPTURE_REQUEST);
    }
    @Override protected void onActivityResult(int req,int result,Intent data){ super.onActivityResult(req,result,data); if(req==CAPTURE_REQUEST && result==RESULT_OK && data!=null){ startWebRTC(data); } else if(req==CAPTURE_REQUEST){ status.setText("Screen capture permission was not granted."); } }
    private void startWebRTC(Intent permission){
        status.setText("Starting screen capture service…");
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());
        factory=PeerConnectionFactory.builder().createPeerConnectionFactory();
        List<PeerConnection.IceServer> ice=new ArrayList<>(); ice.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        PeerConnection.RTCConfiguration cfg=new PeerConnection.RTCConfiguration(ice); cfg.sdpSemantics=PeerConnection.SdpSemantics.UNIFIED_PLAN;
        remoteDescriptionSet=false;
        pendingIceCandidates.clear();
        pendingLocalIceCandidates.clear();
        sentIceCandidateCount=0;
        receivedIceCandidateCount=0;
        generatedIceCandidateCount=0;
        iceConnectionState=PeerConnection.IceConnectionState.NEW;
        iceGatheringState=PeerConnection.IceGatheringState.NEW;
        localAnswerSet=false;
        localAnswerHasIceCredentials=false;
        peer=factory.createPeerConnection(cfg,new PeerConnection.Observer(){
            public void onIceCandidate(IceCandidate c){
                generatedIceCandidateCount++;
                updateIceDiagnostics();
                sendIceCandidate(c);
            }
            public void onDataChannel(DataChannel dc){ dataChannel=dc; setupDataChannel(dc); }
            public void onConnectionChange(PeerConnection.PeerConnectionState s){ runOnUiThread(()->status.setText("WebRTC: "+s)); }
            public void onAddStream(MediaStream s){} public void onRemoveStream(MediaStream s){} public void onSignalingChange(PeerConnection.SignalingState s){}
            public void onIceConnectionChange(PeerConnection.IceConnectionState s){
                iceConnectionState=s;
                updateIceDiagnostics();
            }
            public void onIceConnectionReceivingChange(boolean b){}
            public void onIceGatheringChange(PeerConnection.IceGatheringState s){
                iceGatheringState=s;
                updateIceDiagnostics();
            }
            public void onIceCandidatesRemoved(IceCandidate[] c){} public void onRenegotiationNeeded(){} public void onAddTrack(RtpReceiver r,MediaStream[] s){}
        });
        videoSource=factory.createVideoSource(false);
        capturePermissionData=permission;
        Intent serviceIntent=new Intent(this,ScreenCaptureService.class);
        try{
            status.setText("Starting screen capture service…");
            ContextCompat.startForegroundService(this,serviceIntent);
            if(!bindService(serviceIntent,captureServiceConnection,BIND_AUTO_CREATE)){
                throw new IllegalStateException("Could not bind to the screen capture service.");
            }
            mainHandler.postDelayed(captureServiceBindTimeout,CAPTURE_SERVICE_BIND_TIMEOUT_MS);
        }catch(RuntimeException e){
            capturePermissionData=null;
            status.setText("Could not start screen sharing: "+e.getMessage());
            stopService(serviceIntent);
            tearDownWebRTC();
        }
    }
    private void continueScreenCapture(Intent permission){
        try{
            status.setText("Capture service ready. Starting screen capture…");
            captureService.startCapture(permission,videoSource,
                    ()->status.setText("Screen capture stopped."));
            status.setText("Screen capture running. Connecting to Windows…");
        }catch(RuntimeException e){
            status.setText("Screen capture failed: "+e.getMessage());
            tearDownWebRTC();
            return;
        }
        VideoTrack track=factory.createVideoTrack("syncsphere-screen",videoSource); peer.addTrack(track,new ArrayList<>(Collections.singletonList("syncsphere")));
        dataChannel=peer.createDataChannel("syncsphere",new DataChannel.Init()); setupDataChannel(dataChannel);
        String ws=serverInput.getText().toString().trim();
        status.setText("Screen capture running. Connecting signaling…");
        stomp=new StompClient(ws,"/topic/signal/"+sessionId,new StompClient.Listener(){
            public void onConnected(){
                runOnUiThread(()->status.setText("Signaling connected. Waiting for the Windows session…"));
                try{
                    flushPendingLocalIceCandidates();
                    stomp.sendSignal(sessionId,deviceId,"ANDROID_READY",new JSONObject());
                }
                catch(Exception e){ runOnUiThread(()->status.setText("Could not announce Android session: "+e.getMessage())); }
            }
            public void onMessage(JSONObject m){
                if(deviceId.equals(m.optString("senderId"))) return;
                String type=m.optString("type");
                if(type.equals("OFFER")) runOnUiThread(()->status.setText("Windows session found. Negotiating video…"));
                handleSignal(type,m.optString("payload"));
            }
            public void onError(String e){runOnUiThread(()->status.setText("Signaling error: "+e));}
        }); stomp.connect();
    }
    private void sendIceCandidate(IceCandidate candidate){
        StompClient currentStomp=stomp;
        if(currentStomp==null || !currentStomp.isConnected()){
            pendingLocalIceCandidates.add(candidate);
            return;
        }
        try{
            currentStomp.sendSignal(sessionId,deviceId,"ICE",new JSONObject()
                    .put("sdpMid",candidate.sdpMid)
                    .put("sdpMLineIndex",candidate.sdpMLineIndex)
                    .put("candidate",candidate.sdp));
            sentIceCandidateCount++;
            updateIceDiagnostics();
        }catch(Exception e){
            pendingLocalIceCandidates.add(candidate);
            runOnUiThread(()->status.setText("Could not send ICE candidate: "+e.getMessage()));
        }
    }
    private void updateIceDiagnostics(){
        runOnUiThread(()->status.setText("WebRTC ICE: "+iceConnectionState
                +" | local answer "+(localAnswerSet
                        ?(localAnswerHasIceCredentials?"set with ICE credentials":"set without ICE credentials")
                        :"not set")
                +" | gathering "+iceGatheringState
                +" | generated "+generatedIceCandidateCount
                +" / sent "+sentIceCandidateCount
                +" / received "+receivedIceCandidateCount+" candidates"));
    }
    private void flushPendingLocalIceCandidates(){
        IceCandidate candidate;
        while((candidate=pendingLocalIceCandidates.poll())!=null){
            sendIceCandidate(candidate);
        }
    }
    private void handleSignal(String type,String payload){
        try{ JSONObject p=new JSONObject(payload);
            if(type.equals("OFFER")){ peer.setRemoteDescription(new SimpleSdpObserver(){
                public void onSetSuccess(){
                    remoteDescriptionSet=true;
                    applyPendingIceCandidates();
                    createAnswer();
                }
                public void onSetFailure(String error){ runOnUiThread(()->status.setText("Could not apply Windows offer: "+error)); }
            },new SessionDescription(SessionDescription.Type.OFFER,p.getString("sdp"))); }
            else if(type.equals("ANSWER")){ peer.setRemoteDescription(new SimpleSdpObserver(){
                public void onSetSuccess(){ remoteDescriptionSet=true; applyPendingIceCandidates(); }
                public void onSetFailure(String error){ runOnUiThread(()->status.setText("Could not apply Windows answer: "+error)); }
            },new SessionDescription(SessionDescription.Type.ANSWER,p.getString("sdp"))); }
            else if(type.equals("ICE")){
                IceCandidate candidate=new IceCandidate(p.optString("sdpMid"),p.optInt("sdpMLineIndex"),p.optString("candidate"));
                receivedIceCandidateCount++;
                updateIceDiagnostics();
                if(remoteDescriptionSet) addIceCandidate(candidate);
                else pendingIceCandidates.add(candidate);
            }
        }catch(Exception e){runOnUiThread(()->status.setText("Signal error: "+e.getMessage()));}
    }
    private void applyPendingIceCandidates(){
        for(IceCandidate candidate:pendingIceCandidates) addIceCandidate(candidate);
        pendingIceCandidates.clear();
    }
    private void addIceCandidate(IceCandidate candidate){
        if(peer!=null && !peer.addIceCandidate(candidate)){
            runOnUiThread(()->status.setText("WebRTC could not add a network candidate."));
        }
    }
    private void createAnswer(){
        peer.createAnswer(new SimpleSdpObserver(){
            @Override public void onCreateSuccess(SessionDescription s){
                peer.setLocalDescription(new SimpleSdpObserver(){
                    @Override public void onSetSuccess(){
                        localAnswerSet=true;
                        localAnswerHasIceCredentials=s.description.contains("a=ice-ufrag:")
                                && s.description.contains("a=ice-pwd:");
                        updateIceDiagnostics();
                        try{
                            stomp.sendSignal(sessionId,deviceId,"ANSWER",new JSONObject().put("type","answer").put("sdp",s.description));
                        }catch(Exception e){ runOnUiThread(()->status.setText("Could not send WebRTC answer: "+e.getMessage())); }
                    }
                    @Override public void onSetFailure(String error){ runOnUiThread(()->status.setText("Could not set Android WebRTC answer: "+error)); }
                },s);
            }
            @Override public void onCreateFailure(String error){ runOnUiThread(()->status.setText("Could not create Android WebRTC answer: "+error)); }
        },new MediaConstraints());
    }
    private void setupDataChannel(DataChannel dc){ dc.registerObserver(new DataChannel.Observer(){ public void onBufferedAmountChange(long l){} public void onStateChange(){} public void onMessage(DataChannel.Buffer b){ try{ String text=java.nio.charset.StandardCharsets.UTF_8.decode(b.data).toString(); JSONObject m=new JSONObject(text); String type=m.optString("type"); if(type.equals("TOUCH")){RemoteAccessibilityService.tap((float)m.optDouble("x"),(float)m.optDouble("y"));} else if(type.equals("CLIPBOARD")){setClipboard(m.optString("text"));} else if(type.equals("CLIPBOARD_REQUEST")){sendClipboard();} }catch(Exception ignored){} } }); }
    private void sendClipboard(){ try{if(clipboard.hasPrimaryClip()) sendData(new JSONObject().put("type","CLIPBOARD").put("text",clipboard.getPrimaryClip().getItemAt(0).coerceToText(this).toString()));}catch(Exception ignored){} }
    private void setClipboard(String text){ suppressClipboard=true; clipboard.setPrimaryClip(ClipData.newPlainText("SyncSphere",text)); new Handler().postDelayed(()->suppressClipboard=false,500); }
    private void sendData(JSONObject m){if(dataChannel!=null && dataChannel.state()==DataChannel.State.OPEN){byte[] bytes=m.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);dataChannel.send(new DataChannel.Buffer(java.nio.ByteBuffer.wrap(bytes),false));}}
    private void tearDownWebRTC(){
        mainHandler.removeCallbacks(captureServiceBindTimeout);
        if(stomp!=null){stomp.close();stomp=null;}
        if(peer!=null){peer.close();peer=null;}
        remoteDescriptionSet=false;
        pendingIceCandidates.clear();
        pendingLocalIceCandidates.clear();
        if(videoSource!=null){videoSource.dispose();videoSource=null;}
        if(captureServiceBound){
            captureService.stopCapture();
            unbindService(captureServiceConnection);
            captureServiceBound=false;
            captureService=null;
        }else{
            stopService(new Intent(this,ScreenCaptureService.class));
            capturePermissionData=null;
        }
    }
    @Override protected void onDestroy(){tearDownWebRTC();super.onDestroy();}
    static class SimpleSdpObserver implements SdpObserver { public void onCreateSuccess(SessionDescription s){} public void onSetSuccess(){} public void onCreateFailure(String s){} public void onSetFailure(String s){} }
}
