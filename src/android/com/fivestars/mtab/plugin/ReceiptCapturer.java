package com.fivestars.mtab.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;Intent intent = new Intent(VirtualPrinter.ACTION_RECEIPT_READ);
			//ToDo (bbil): Send receipt data as well
			service.sendBroadcast(intent);

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Intent;

import android.util.Log;

public class ReceiptCapturer implements Runnable {

	// Assume printer spoof will print receipt, order tickets & order ticket
	// stubs
	private int error_count = 0;
	private volatile static boolean shouldRun = true;
	private ServerSocket server = null;
	private Socket client = null;
	private boolean popupCalled = false;

	private MPAIntentService service;

	public ReceiptCapturer(MPAIntentService service) {
		this.service = service;
	}

	public void terminate() {
		Log.d("VirtualPrinter", "VirtualPrinter", "Killing Receipt Capturer Thread @ "
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
		Log.d("VirtualPrinter", "VirtualPrinter", "Restarting Receipt Capturer Thread @ "
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
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		}

		Log.d("VirtualPrinter", "Connect to port 9100 @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));

		while (shouldRun) {
			popupCalled = false;
			intermediateSteps = 0;

			Log.e("MSG",
					"MSG - LOOKING FOR CONNECTION @ "
							+ System.currentTimeMillis());
			try {
				client = server.accept();
			} catch (IOException e) {
				Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
				restart();
				return;
			}

			Log.d("VirtualPrinter", "Accepted TCP Connection @"
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));

			Intent intentConnect = new Intent(VirtualPrinter.ACTION_RECEIPT_READ);
			service.sendBroadcast(intentConnect);

			processInput(client);

			try {
				server.setSoTimeout(1000);
			} catch (SocketException e) {
				Log.d("VirtualPrinter", e.toString()
						+ " "
						+ e.getMessage()
						+ " @ "
						+ new SimpleDateFormat("yyyyMMdd_HHmss")
								.format(new Date()));
			}

			do {
				try {
					client = server.accept();
					Log.d("VirtualPrinter", "Accepted Intermediate TCP Connection @"
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));
					// processInput(client);
				} catch (IOException e) {
					Log.d("VirtualPrinter", "TIMEOUT "
							+ e.toString()
							+ " @ "
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));
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
				Log.d("VirtualPrinter", e2.toString()
						+ " "
						+ e2.getMessage()
						+ " @ "
						+ new SimpleDateFormat("yyyyMMdd_HHmss")
								.format(new Date()));
			}
		}
	}

	public void processInput(Socket client) {
		Log.d("VirtualPrinter", "Processing TCP Input @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
		
		byte[] message = MockPrinter.hexStringToByteArray("2386020000000004000000");
		byte[] message_2 = MockPrinter.hexStringToByteArray("2386000000000000000000");

		InputStream in = null;
		OutputStream out = null;
		
		try {
			in =  client.getInputStream();
			out = client.getOutputStream();

			out.write(message);
			
			byte[] buffer = new byte[1480];
			List<Byte> bytes = new ArrayList<Byte>();
			int read;
			Log.d("VirtualPrinter", "Starting to read @ " + new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
			while((read = in.read(buffer,0,buffer.length)) != -1){
				Log.d("VirtualPrinter", "Read a line of Input @"
						+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
				for (int i = 0; i < buffer.length; i++){
					if (i == 0 && buffer[0] == (byte)27 && buffer[1] == (byte)42 && buffer[2] == (byte)114 && buffer[3] == (byte)66){
						out.write(message_2);
						break;
					}
					if (i == 0 && buffer[0] == (byte)27 && buffer[1] == (byte)6 && buffer[2] == (byte)1){
						out.write(message);
						break;
					}
					
					if (i == 0 && buffer[0]== (byte)27 && buffer[1]==(byte)29){
						out.write(message_2);
						break;
					}
			
					bytes.add(buffer[i]);
					buffer[i] = 0;
				}
				Log.d("VirtualPrinter", "Finish reading a line of Input @"
						+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
			}
			
			/*
			byte[] message = new byte[bytes.size()];
			for (int i = 0; i < bytes.size(); i++){
				message[i] = bytes.get(i);
			}
			*/
			
			Log.d("VirtualPrinter", "Read TCP Input @ "
					+new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));

			Intent intent = new Intent(VirtualPrinter.ACTION_RECEIPT_READ);
			//ToDo (bbil): Send receipt data as well
			service.sendBroadcast(intent);

			//callServer();
			//OCR.receiptImageGen(message);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		
		try {
			in.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Log.d("VirtualPrinter", "Finished Processing Input @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));

	}

	public void callServer() {
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet request = new HttpGet();

		try {
			request.setURI(new URI("http://apns-mtab.herokuapp.com"));
			httpclient.execute(request);
			popupCalled = true;
		} catch (URISyntaxException e) {
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		} catch (ClientProtocolException e) {
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		} catch (IOException e) {
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		}
		Log.d("VirtualPrinter", "Made call to Heroku Server @"
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));
	}
}