package com.fivestars.mtab.plugin;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.util.Log;

public class MPAIntentService extends IntentService {

	static boolean isFinishing = false;
	final MockPrinter mp;
	final ReceiptCapturer rc;
	final int TIME_TO_SLEEP = 1000;

	public MPAIntentService() {
		super("MPAIntentService");
		Log.d("virtualPrinter", "MPAIntentService constructor");

		rc = new ReceiptCapturer(this);
		mp = new MockPrinter(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		Log.d("virtualPrinter", "MPAIntentService started");

		isFinishing = false;

		final Thread wifiCheck = new Thread() {
			Thread mockPrinter = new Thread(mp);
			Thread virtualPrinter = new Thread(rc);

			public void run() {

				boolean wifiDisabled = false;
				boolean firstCall = true;

				ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				while (true) {
					if (!mWifi.isConnected() && !wifiDisabled) {
						/*
						MainActivity.log("WiFi Disabled! Killing threads @ "
								+ new SimpleDateFormat("yyyyMMdd_HHmss")
										.format(new Date()));*/
						wifiDisabled = true;
						firstCall = true;
						if (mp.isRunning()) {
							mp.terminate();
							if (mockPrinter.isAlive()) {
								mockPrinter.interrupt();
							}
							mockPrinter = new Thread(mp);
						}
						if (rc.isRunning()) {
							rc.terminate();
							if (virtualPrinter.isAlive()) {
								virtualPrinter.interrupt();
							}
							virtualPrinter = new Thread(rc);
						}
						System.gc();
					}

					else if (mWifi.isConnected()) {
						if (wifiDisabled) {
							/*
							MainActivity
									.log("WiFi Enabled! Starting Threads @ "
											+ new SimpleDateFormat(
													"yyyyMMdd_HHmss")
													.format(new Date()));
													*/
							wifiDisabled = false;
							firstCall = false;
							if (!mp.isRunning()) {
								mockPrinter.start();
							}

							if (!rc.isRunning()) {
								virtualPrinter.start();
							}
						} else if (firstCall) {
							/*
							MainActivity.log("Starting threads @ "
									+ new SimpleDateFormat("yyyyMMdd_HHmss")
											.format(new Date()));*/
							firstCall = false;
							mockPrinter.start();
							virtualPrinter.start();
						}
					}
					
					try {
						Thread.sleep(TIME_TO_SLEEP);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		/*
		MainActivity.log("Service started @ "
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/
		wifiCheck.start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("virtualPrinter", "MPAIntentService started");
		onHandleIntent(intent);
		return START_STICKY;
	}

	public void restartService() {
		Context context = this.getApplicationContext();

		Intent intent = new Intent(context, MPAIntentService.class);
		context.stopService(intent);
		context.startService(intent);
	}
}
