package com.auction.client;

import java.net.*;
import java.util.Enumeration;

public class ServerDiscoveryClient {

    private static final int DISCOVERY_PORT = 8888;
    private static final String DISCOVERY_MSG = "AUCTION_DISCOVER";
    private static final String RESPONSE_PREFIX = "AUCTION_SERVER:";
    private static final int TIMEOUT_MS = 3000; // chờ 3 giây mỗi lượt

    /**
     * Tìm server trong LAN bằng UDP broadcast.
     * Gửi broadcast lên 255.255.255.255 VÀ địa chỉ broadcast của từng
     * network interface để tăng khả năng tìm thấy server khi router
     * chặn limited broadcast.
     *
     * @return mảng [host, port] nếu tìm thấy, null nếu không
     */
    public static String[] discover() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_MS);

            byte[] sendBuf = DISCOVERY_MSG.getBytes();

            // 1. Gửi limited broadcast (255.255.255.255)
            sendBroadcast(socket, sendBuf, "255.255.255.255");

            // 2. Gửi thêm subnet broadcast của từng network interface
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces != null && interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    // Bỏ qua interface loopback, không hoạt động, hoặc không hỗ trợ multicast
                    if (ni.isLoopback() || !ni.isUp()) continue;
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        InetAddress broadcast = ia.getBroadcast();
                        if (broadcast != null) {
                            sendBroadcast(socket, sendBuf, broadcast.getHostAddress());
                        }
                    }
                }
            } catch (SocketException ignored) {}

            // 3. Chờ phản hồi từ bất kỳ broadcast nào ở trên
            byte[] recvBuf = new byte[256];
            DatagramPacket response = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(response); // ném SocketTimeoutException nếu quá TIMEOUT_MS

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

    private static void sendBroadcast(DatagramSocket socket, byte[] data, String broadcastAddr) {
        try {
            InetAddress addr = InetAddress.getByName(broadcastAddr);
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, DISCOVERY_PORT);
            socket.send(packet);
        } catch (Exception e) {
            // Không in lỗi — một số interface có thể không hỗ trợ, bỏ qua
        }
    }
}