package com.fivestars.mtab.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import com.acs.smartcard.Reader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public class PrinterBroadcastReceiver extends BroadcastReceiver{
    private CallbackContext callbackContext;
    private Activity activity;
    private boolean[] permissionCount;
    private CallbackContext detachCallback;
     
    private static final String ACTION_PRINT_RECEIVED = "print_received";
    
    public PrinterBroadcastReciever(CallbackContext callbackContext, Activity activity) {
        this.callbackContext = callbackContext;
        this.activity = activity;
    }
     
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //Bundle extras = intent.getExtras();
        if (ACTION_PRINT_RECEIVED.equals(action)) {
             synchronized (this) {

                String data = "blargh";

                callbackContext.success(data);
            }
        }
    }
}