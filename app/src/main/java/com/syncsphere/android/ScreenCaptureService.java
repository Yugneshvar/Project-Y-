package com.syncsphere.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ServiceCompat;

import org.webrtc.EglBase;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL_ID = "syncsphere_screen_capture";
    private static final int NOTIFICATION_ID = 1001;
    private final IBinder binder = new LocalBinder();
    private EglBase eglBase;
    private SurfaceTextureHelper surfaceTextureHelper;
    private ScreenCapturerAndroid capturer;
    private boolean captureStarted;

    public final class LocalBinder extends Binder {
        public ScreenCaptureService getService() {
            return ScreenCaptureService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForegroundService();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void startCapture(Intent permissionData, VideoSource videoSource,
            Runnable onProjectionStopped) {
        startAsForegroundService();

        eglBase = EglBase.create();
        surfaceTextureHelper = SurfaceTextureHelper.create(
                "SyncSphereCapture", eglBase.getEglBaseContext());
        capturer = new ScreenCapturerAndroid(permissionData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                captureStarted = false;
                new Handler(Looper.getMainLooper()).post(onProjectionStopped);
                stopCapture();
            }
        });
        capturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        try {
            capturer.startCapture(1280, 720, 15);
            captureStarted = true;
        } catch (RuntimeException e) {
            releaseCaptureResources();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            throw e;
        }
    }

    public void stopCapture() {
        try {
            releaseCaptureResources();
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private void startAsForegroundService() {
        int serviceType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                : 0;
        ServiceCompat.startForeground(
                this, NOTIFICATION_ID, createNotification(), serviceType);
    }

    private Notification createNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("SyncSphere screen sharing")
                .setContentText("Your screen is being shared with the paired device.")
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Screen sharing",
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private void releaseCaptureResources() {
        if (capturer != null) {
            try {
                if (captureStarted) {
                    capturer.stopCapture();
                }
            } finally {
                captureStarted = false;
                capturer.dispose();
                capturer = null;
            }
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
    }

    @Override
    public void onDestroy() {
        releaseCaptureResources();
        super.onDestroy();
    }
}
