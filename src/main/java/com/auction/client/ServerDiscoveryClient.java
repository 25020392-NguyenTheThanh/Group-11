package com.auction.client;

import java.net.*;

public class ServerDiscoveryClient {

    private static final int DISCOVERY_PORT = 8888;
    private static final String DISCOVERY_MSG = "AUCTION_DISCOVER";
    private static final String RESPONSE_PREFIX = "AUCTION_SERVER:";
    private static final int TIMEOUT_MS = 3000; // chờ 3 giây

     // Tìm server trong LAN.
     // @return mảng [host, port] nếu tìm thấy, null nếu không

    public static String[] discover() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_MS);

            // Gửi broadcast hỏi server
            byte[] sendBuf = DISCOVERY_MSG.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    sendBuf, sendBuf.length,
                    InetAddress.getByName("255.255.255.255"),
                    DISCOVERY_PORT
            );
            socket.send(packet);

            // Chờ phản hồi
            byte[] recvBuf = new byte[256];
            DatagramPacket response = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(response); // ném SocketTimeoutException nếu quá 3s

            String msg = new String(response.getData(), 0, response.getLength()).trim();
            if (msg.startsWith(RESPONSE_PREFIX)) {
                String serverHost = response.getAddress().getHostAddress();
                String serverPort = msg.substring(RESPONSE_PREFIX.length());
                System.out.println("Tìm thấy server: " + serverHost + ":" + serverPort);
                return new String[]{serverHost, serverPort};
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Không tìm thấy server trong LAN sau " + TIMEOUT_MS + "ms");
        } catch (Exception e) {
            System.err.println("Discovery lỗi: " + e.getMessage());
        }
        return null;
    }
}