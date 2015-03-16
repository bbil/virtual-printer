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
import java.util.HashMap;

public class MockPrinter implements Runnable {

	private volatile static boolean shouldRun = true;
	private DatagramSocket sock;

	// hardcoded square printer port
	final private int LISTEN_TO_UDP_PORT = 22222;
	private InetAddress LISTEN_TO_UDP_IP = null;

	final int ID_INDEX = 79; // the index of ID in the message packet
	final int ID_LENGTH = 4;

	private int error_count = 0;

	/*
	 * hardcoded msg we want to send out to make sure we are detected as a
	 * printer in Square
	 */
	private byte[] message = hexStringToByteArray("5354525f4243415354000000000000005253312e302e3100012e00220074008a012c00525453503130304c420e000000000000003330302e3232300031303000000000000000000000000000310000116207cb8200000000c0a8006644484350000000000000000000000000ffffff00c0a800010016000000000000000000000000000030000000300000a25374617200000000000000000000000000000000000000000000000000000000535441520000000000000000000000000000000000000000000000000000000054535031343320285354525f542d30303129000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005052494e544552000000000000000000000000000000000000000000000000000002");

	public void terminate() {
		/*
		MainActivity.log("Killing Mock Printer Thread @ "
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/

		shouldRun = false;

		if (sock != null && !sock.isClosed()) {
			sock.close();
		}
	}

	public void restart() {
		/*
		MainActivity.log("Restarting Mock Printer thread @ "
				+ new SimpleDateFormat("yyyyMMdd_HHmss").format(new Date()));*/

		if (error_count >= 5) {
			VirtualPrinter.restartService();
			return;
		}
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
			/*
			MainActivity.log(e.toString() + " " + e.getMessage());
			*/
			restart();
			return;
		}

		DatagramChannel channel = null;
		try {
			channel = DatagramChannel.open();
		} catch (IOException e) {
			//MainActivity.log(e.toString() + " " + e.getMessage());
			restart();
			return;
		}

		try {
			sock = channel.socket();
			sock.setReuseAddress(true);
			sock.bind(new InetSocketAddress(LISTEN_TO_UDP_IP,
					LISTEN_TO_UDP_PORT));
		} catch (SocketException e) {
			//MainActivity.log(e.toString() + " " + e.getMessage());
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
				//MainActivity.log(e.toString() + " " + e.getMessage());
				restart();
				return;
			}

			InetAddress SEND_TO_UDP_IP = packet.getAddress();
			int SEND_TO_UDP_PORT = packet.getPort();

			/*
			MainActivity
			.log("Received UDP Packet from " + SEND_TO_UDP_IP +" @ "
					+ new SimpleDateFormat("yyyyMMdd_HHmss")
							.format(new Date()));*/
			byte[] hash = SEND_TO_UDP_IP.getAddress();

			for (int i = 0; i < ID_LENGTH; i++) {
				message[ID_INDEX + i] = hash[i];
			}

			message_p.setData(message);
			message_p.setAddress(SEND_TO_UDP_IP);
			message_p.setPort(SEND_TO_UDP_PORT);

			try {
				sock.send(message_p);
			} catch (IOException e) {
				//MainActivity.log(e.toString() + " " + e.getMessage());
				restart();
				return;
			}

			/*
			MainActivity
					.log("Sent UDP Response to " +SEND_TO_UDP_IP +" @ "
							+ new SimpleDateFormat("yyyyMMdd_HHmss")
									.format(new Date()));*/

			error_count = 0;
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
}
