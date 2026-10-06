package com.syncsphere.android;

import okhttp3.*;
import okio.ByteString;
import org.json.JSONObject;

public class StompClient {
    public interface Listener { void onConnected(); void onMessage(JSONObject body); void onError(String error); }
    private final OkHttpClient client = new OkHttpClient();
    private final String url;
    private final String topic;
    private final Listener listener;
    private WebSocket ws;
    private boolean connected;

    public StompClient(String url, String topic, Listener listener) { this.url=url; this.topic=topic; this.listener=listener; }
    public void connect() {
        Request request = new Request.Builder().url(url).build();
        ws = client.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                sendRaw("CONNECT\naccept-version:1.2\nhost:syncsphere\n\n\u0000");
            }
            @Override public void onMessage(WebSocket webSocket, String text) { parse(text); }
            @Override public void onMessage(WebSocket webSocket, ByteString bytes) { parse(bytes.utf8()); }
            @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) { listener.onError(t.getMessage()==null?"WebSocket failed":t.getMessage()); }
            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                connected=false;
                listener.onError("Signaling connection closed ("+code+"): "+reason);
            }
        });
    }
    private void parse(String frame) {
        frame=frame.replace("\r\n","\n");
        while(frame.startsWith("\n")) frame=frame.substring(1);
        if (frame.contains("CONNECTED")) {
            connected=true;
            sendRaw("SUBSCRIBE\nid:syncsphere-sub-1\ndestination:"+topic+"\nack:auto\n\n\u0000");
            listener.onConnected();
            return;
        }
        if(frame.startsWith("ERROR")) {
            int bodyStart=frame.indexOf("\n\n");
            String detail=bodyStart>=0?frame.substring(bodyStart+2).replace("\u0000","").trim():"";
            connected=false;
            listener.onError(detail.isEmpty()?"STOMP signaling server rejected the connection":detail);
            return;
        }
        int bodyStart=frame.indexOf("\n\n");
        if (frame.startsWith("MESSAGE") && bodyStart>=0) {
            String body=frame.substring(bodyStart+2).replace("\u0000", "").trim();
            try { listener.onMessage(new JSONObject(body)); } catch(Exception e) { listener.onError("Invalid signaling message"); }
        }
    }
    public void sendSignal(String sessionId, String senderId, String type, JSONObject payload) {
        if (!connected) return;
        try {
            JSONObject msg=new JSONObject(); msg.put("sessionId",sessionId); msg.put("senderId",senderId); msg.put("type",type); msg.put("payload",payload.toString());
            String body=msg.toString();
            sendRaw("SEND\ndestination:/app/signal\ncontent-type:application/json\ncontent-length:"+body.getBytes().length+"\n\n"+body+"\u0000");
        } catch(Exception e) { listener.onError(e.getMessage()); }
    }
    public boolean isConnected(){ return connected; }
    private void sendRaw(String frame){ if(ws!=null) ws.send(frame); }
    public void close(){ connected=false; if(ws!=null) ws.close(1000,"bye"); client.dispatcher().executorService().shutdown(); }
}
