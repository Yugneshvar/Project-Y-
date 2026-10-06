package com.syncsphere.android;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class RemoteAccessibilityService extends AccessibilityService {
    private static RemoteAccessibilityService instance;
    @Override public void onServiceConnected(){ instance=this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
    public static boolean tap(float nx,float ny){
        if(instance==null) return false;
        android.util.DisplayMetrics dm=instance.getResources().getDisplayMetrics();
        float x=Math.max(0,Math.min(dm.widthPixels-1,nx*dm.widthPixels));
        float y=Math.max(0,Math.min(dm.heightPixels-1,ny*dm.heightPixels));
        Path p=new Path(); p.moveTo(x,y);
        GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,80)).build();
        return instance.dispatchGesture(g,null,null);
    }
}
