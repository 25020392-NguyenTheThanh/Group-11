package com.auction.server;

import com.auction.client.ServerConnection;

import java.net.*;

public class ServerDiscovery implements Runnable {

    private static final int DISCOVERY_PORT = 8888;
    private static final String DISCOVERY_MSG = "AUCTION_DISCOVER";
    private static final String RESPONSE_PREFIX = "AUCTION_SERVER:";

    private volatile boolean running = true;

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            socket.setBroadcast(true);
            byte[] buf = new byte[256];
            System.out.println("Discovery service đang chạy trên port " + DISCOVERY_PORT);

            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet); // chờ client hỏi

                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                if (DISCOVERY_MSG.equals(msg)) {
                    // Trả lời cho client biết đây là server auction
                    String response = RESPONSE_PREFIX + ServerConnection.DEFAULT_PORT; // port TCP của server
                    byte[] responseBytes = response.getBytes();
                    DatagramPacket reply = new DatagramPacket(
                            responseBytes, responseBytes.length,
                            packet.getAddress(), packet.getPort()
                    );
                    socket.send(reply);
                    System.out.println("Đã trả lời discovery từ: " + packet.getAddress());
                }
            }
        } catch (Exception e) {
            System.err.println("Discovery error: " + e.getMessage());
        }
    }
}