package com.fivestars.mtab.plugin;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class VirtualPrinter extends CordovaPlugin {

    private static final String ACTION_READ_CALLBACK = "registerReadCallback";
    private static final String ACTION_INIT = "init";

    public static final String ACTION_RECEIPT_READ = "receiptRead";

    private static Intent serviceIntent;

    private BroadcastReceiver mReceiver;
    
    private CallbackContext readCallback;
    private CallbackContext detachCallback;
    
    private boolean[] permissionCount = new boolean[1];

    private String TAG = "NFC";
   
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (ACTION_READ_CALLBACK.equals(action)) {
            registerReadCallback(callbackContext);
            return true;
        }
        

        else if (ACTION_INIT.equals(action)) {
            init(callbackContext);
            return true;
        }
        
        // the action doesn't exist
        return false;
    }

    private void registerReadCallback(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                readCallback = callbackContext;
                JSONObject returnObj = new JSONObject();
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
    }

    private void init(final CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                //Start the service with an Intent, register a BroadcastReceiver

                serviceIntent= new Intent(cordova.getActivity().getApplicationContext(), MPAIntentService.class);

                mReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        if (readCallback != null) {
                            JSONObject returnObj = new JSONObject();
                            //insert data here. for now, all we need is the fact a receipt was received
                            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                            pluginResult.setKeepCallback(true);
                            readCallback.sendPluginResult(pluginResult);
                        }

                    }
                };

                Context context = cordova.getActivity().getApplicationContext();

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ACTION_RECEIPT_READ);
                context.registerReceiver(mReceiver, intentFilter);

                context.startService(serviceIntent);

                if (callback != null) {
                    callback.success();
                }

            }
        });
    }
}
