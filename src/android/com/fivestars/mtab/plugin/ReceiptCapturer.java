package com.fivestars.mtab.plugin;

import android.content.Intent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class ReceiptCapturer implements Runnable {

	// Assume printer spoof will print receipt, order tickets & order ticket
	// stubs
	private int error_count = 0;
	private volatile static boolean shouldRun = true;
	private ServerSocket server = null;
	private Socket client = null;
	private boolean popupCalled = false;
	final private byte RECEIPT_INDICATOR = 7; // the data from the packet that
												// indicates receipt to be
												// printed

	private MPAIntentService service;

	public ReceiptCapturer(MPAIntentService service) {
		this.service = service;
	}

	public void terminate() {
		/*
		MainActivity.log("Killing Receipt Capturer Thread @ "
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/
		Log.d("VirtualPrinter", "Killing Receipt Capturer Thread @ "
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
		shouldRun = false;

		if (server != null && !server.isClosed()) {
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (client != null && !client.isClosed()) {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void restart() {
		/*
		MainActivity.log("Restarting Receipt Capturer Thread @ "
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/
		Log.d("VirtualPrinter", "Restarting Receipt Capturer Thread @ "
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
		if (error_count >= 5) {
			service.restartService();
			return;
		}
		error_count++;
		terminate();
		run();
	}

	public static Boolean isRunning() {
		return shouldRun;
	}

	@Override
	public void run() {
		shouldRun = true;
		int intermediateSteps = 0;

		try {
			server = new ServerSocket(9100);
			server.setReceiveBufferSize(7300);
		} catch (IOException e) {
			//MainActivity.log(e.toString() + " " + e.getMessage());
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		}

		/*
		MainActivity.log("Connect to port 9100 @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/
		Log.d("VirtualPrinter", "Connect to port 9100 @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));

		while (shouldRun) {
			popupCalled = false;
			intermediateSteps = 0;

			try {
				client = server.accept();
			} catch (IOException e) {
				//MainActivity.log(e.toString() + " " + e.getMessage());
				Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
				restart();
				return;
			}

			Intent intentConnect = new Intent(VirtualPrinter.ACTION_CONNECT_PRINTER);
			//ToDo (bbil): Send receipt data as well
			service.sendBroadcast(intentConnect);

			/*
			MainActivity
					.log("Accepted TCP Connection @"
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));*/
			Log.d("VirtualPrinter", "Accepted TCP Connection @"
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));

			processInput(client);

			try {
				server.setSoTimeout(1000);
			} catch (SocketException e) {
				//MainActivity.log(e.toString() + " " + e.getMessage() + " @ "+  new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
				Log.d("VirtualPrinter", e.toString() + " " + e.getMessage() + " @ "
					+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
			}

			do {
				try {
					client = server.accept();
					/*
					MainActivity.log("Accepted Intermediate TCP Connection @"
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));*/
					Log.d("VirtualPrinter", "Accepted Intermediate TCP Connection @"
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));
					processInput(client);
				} catch (IOException e) {
					//MainActivity.log("TIMEOUT " + e.toString() + " @ "+  new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
					Log.d("VirtualPrinter", "TIMEOUT " + e.toString() + " @ "+  new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
					client = null;
				}
				intermediateSteps++;
			} while (client != null);

			/*
			 * This is in case someone turns off receipts being printed. This
			 * will launch a popup whenever we see an order ticket printed. We
			 * keep track of intermediate steps since order ticket stubs prints
			 * whenever order ticket prints
			 */
			if (intermediateSteps > 1 && !popupCalled) {
				/*MainActivity.log("Launched Popup due to Order Ticket TCP @"
						+ new SimpleDateFormat("yyyyMMdd_HHmss")
								.format(new Date()));*/
				Log.d("VirtualPrinter", "Launched Popup due to Order Ticket TCP @"
						+ new SimpleDateFormat("yyyyMMdd_HHmss")
								.format(new Date()));
				//callServer();

				Intent intent = new Intent(VirtualPrinter.ACTION_RECEIPT_READ);
				//ToDo (bbil): Send receipt data as well
				service.sendBroadcast(intent);
			}

			try {
				server.setSoTimeout(0);
			} catch (SocketException e2) {
				//MainActivity.log(e2.toString() + " " + e2.getMessage() + " @ "+  new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
				Log.d("VirtualPrinter", e2.toString() + " " + e2.getMessage() + " @ "+  new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
			}
		}
	}

	public void processInput(Socket client) {
		/*
		MainActivity.log("Processing TCP Input @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/
		Log.d("VirtualPrinter", "Processing TCP Input @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));

		InputStream inp = null;

		try {
			inp = client.getInputStream();
		} catch (IOException e) {
			//MainActivity.log(e.toString() + " " + e.getMessage());
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(inp));
		byte temp = 0;

		try {
			temp = (byte) in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (temp == RECEIPT_INDICATOR && !popupCalled) {
			/*
			MainActivity
					.log("Launched Popup due to Receipt TCP @"
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));*/
			Log.d("VirtualPrinter", "Launched Popup due to Receipt TCP @"
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));
			popupCalled = true;
			//callServer();

			Intent intent = new Intent(VirtualPrinter.ACTION_RECEIPT_READ);
			//ToDo (bbil): Send receipt data as well
			service.sendBroadcast(intent);
		}

		String x = "";

		do {
			try {
				x = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (x != null);

		try {
			in.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		/*
		MainActivity.log("Finished Processing Input @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/
		Log.d("VirtualPrinter", "Finished Processing Input @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));

	}

	public void callServer() {

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet request = new HttpGet();

		try {
			request.setURI(new URI("http://apns-mtab.herokuapp.com"));
			httpclient.execute(request);
		} catch (URISyntaxException e) {
			//MainActivity.log(e.toString() + " " + e.getMessage());
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		} catch (ClientProtocolException e) {
			//MainActivity.log(e.toString() + " " + e.getMessage());
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		} catch (IOException e) {
			//MainActivity.log(e.toString() + " " + e.getMessage());
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		}
		/*
		MainActivity.log("Made call to Heroku Server @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/
		Log.d("VirtualPrinter", "Made call to Heroku Server @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
	}
}
