package com.fivestars.mtab.plugin;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

public class MockPrinter implements Runnable {

	private volatile static boolean shouldRun = true;
	private DatagramSocket sock;

	// hardcoded square printer port
	final private int LISTEN_TO_UDP_PORT = 22222;
	private InetAddress LISTEN_TO_UDP_IP = null;

	final static int ID_INDEX = 79; // the index of ID in the message packet
	final static int ID_LENGTH = 4;
	final static int IP_INDEX = ID_INDEX + 9;
	final static int SUB_INDEX = IP_INDEX + 20;
	final static int GATE_INDEX = SUB_INDEX + 4;
	
	private int error_count = 0;

	private byte[] message = hexStringToByteArray(createMessage());

	private MPAIntentService service;

	public MockPrinter(MPAIntentService service) {
		this.service = service;
	}

	public void terminate() {
		Log.d("VirtualPrinter", "Killing Mock Printer Thread @ "
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));

		shouldRun = false;

		if (sock != null && !sock.isClosed()) {
			sock.close();
		}
	}

	public void restart() {
		Log.d("VirtualPrinter", "Restarting Mock Printer thread @ "
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

		try {
			LISTEN_TO_UDP_IP = InetAddress.getByName("0.0.0.0");
		} catch (Exception e) {
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		}

		DatagramChannel channel = null;
		try {
			channel = DatagramChannel.open();
		} catch (IOException e) {
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		}

		try {
			sock = channel.socket();
			sock.setReuseAddress(true);
			sock.bind(new InetSocketAddress(LISTEN_TO_UDP_IP,
					LISTEN_TO_UDP_PORT));
		} catch (SocketException e) {
			Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
			restart();
			return;
		}

		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, 1024);
		DatagramPacket message_p = new DatagramPacket(message, message.length);

		while (true) {
			for (int i = 0; i < 1024; i++) {
				buffer[i] = 0;
			}

			packet.setData(buffer);

			try {
				sock.receive(packet);
			} catch (IOException e) {
				Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
				restart();
				return;
			}

			InetAddress SEND_TO_UDP_IP = packet.getAddress();
			int SEND_TO_UDP_PORT = packet.getPort();

			/*
			MainActivity
					.log("Received UDP Packet from "
							+ SEND_TO_UDP_IP
							+ " @ "
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));
			*/
			
			byte[] hash = SEND_TO_UDP_IP.getAddress();


			for (int i = 0; i < ID_LENGTH; i++) {
				message[ID_INDEX + i] = hash[i];
				/*
				message[IP_INDEX + i] = MainActivity.ip[i];
				message[SUB_INDEX + i] = MainActivity.subnet[i];
				message[GATE_INDEX + i] = MainActivity.gateway[i];
				*/
			}

			message_p.setData(message);
			message_p.setAddress(SEND_TO_UDP_IP);
			message_p.setPort(SEND_TO_UDP_PORT);

			try {
				sock.send(message_p);
			} catch (IOException e) {
				Log.d("VirtualPrinter", e.toString() + " " + e.getMessage());
				restart();
				return;
			}

			/*
			MainActivity
					.log("Sent UDP Response to "
							+ SEND_TO_UDP_IP
							+ " @ "
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));
									*/

		}
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String createMessage() {
		String print_app = "SQUARE";

		StringBuilder temp = new StringBuilder(1000000);
		temp.append("5354525f4243415354000000000000005253312e302e3100012e00220074008a012c0052545350313030");
		temp.append(print_app.equals("SHOPKEEP") ? "4c414e" : "4c420e");

		temp.append("000000000000003330302e32323000313030000000000000000000000000003100");

		// placeholder to append in MAC Address (way to identify printer)
		temp.append("ffffffffffff");

		temp.append("00000000");

		// placeholder append IP address of machine
		temp.append("ffffffff");

		temp.append("44484350000000000000000000000000");

		// placeholder to append subnet mask
		temp.append("ffffffff");

		// placeholder to append default gateway address
		temp.append("ffffffff");

		temp.append("00");

		temp.append(print_app.equals("SHOPKEEP") ? "17" : "16");

		temp.append("000000000000000000000000000030000000300000a25374617200000000000000000000000000000000000000000000000000000000535441520000000000000000000000000000000000000000000000000000000054535031343320285354525f542d30303129000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005052494e544552000000000000000000000000000000000000000000000000000002");
		return temp.toString();
	}
}
