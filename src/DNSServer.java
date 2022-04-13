import java.io.*;
import java.net.*;

public class DNSServer {
    DatagramPacket receivePacket;
    DatagramPacket googlePacket;
    DatagramPacket sendPacket;

    static DatagramSocket server_socket = null;

    InetAddress clientAddress;
    int clientPort;

    static int debug = 5;

    public static void main(String[] args) throws IOException {

        final int PORT_NUMBER = 8053;


        // Open a socket on PORT_NUMBER
        try {
            server_socket = new DatagramSocket(PORT_NUMBER);
        } catch (SocketException e) {
            if (debug > 1) {
                e.printStackTrace();
            }
            System.out.println("Unable to create server socket. Program will now exit.");
            System.exit(1);
        }

        byte[] receiveData = new byte[1024];
        byte[] googleData = new byte[1024];
        byte[] sendData;

        while (true) { // Start listening for requests
            DNSServer server = new DNSServer();

            // Receive
            server.receivePacket = new DatagramPacket(receiveData, receiveData.length);
            server_socket.receive(server.receivePacket);

            // Collect client data
            server.clientAddress = server.receivePacket.getAddress();
            server.clientPort = server.receivePacket.getPort();

            // Parse Packet
            DNSMessage request = DNSMessage.decodeMessage(receiveData);

            if (DNSCache.isCached(request)) {
                respond(server, request);

            } else {
                InetAddress googleDNS = InetAddress.getByName("8.8.8.8");
                DatagramSocket googleSocket = new DatagramSocket();

                // Forward request to Google
                if (debug > 0) {
                    System.out.println("Sending packet to Google!");
                }
                server.sendPacket = new DatagramPacket(receiveData, server.receivePacket.getLength(), googleDNS,
                        53); // It is important to send conformed byte length of the packet instead of the array length.
                googleSocket.send(server.sendPacket);
                if (debug > 0) {
                    System.out.println("Done, sent the packet to Google!\n");
                }

                // Prepare to receive answer from Google
                if (debug > 0) {
                    System.out.println("Preparing to receive answer from Google!");
                }
                server.googlePacket = new DatagramPacket(googleData, googleData.length);
                googleSocket.receive(server.googlePacket);
                if (debug > 0) {
                    System.out.println("Received answer from Google!\n");
                }

                DNSMessage googleMessage = DNSMessage.decodeMessage(googleData);

                // Put data in cache
                DNSCache.insertRecord(request, googleMessage);

                // Respond back
                respond(server, request);

            }


        }
    }

    private static void respond(DNSServer server, DNSMessage request) throws IOException {
        byte[] sendData;
        DNSRecord[] answers = new DNSRecord[1];
        answers[0] = DNSCache.fetchRecord(request);
        DNSMessage response = DNSMessage.buildResponse(request, answers);
        sendData = response.toBytes();

        server.sendPacket = new DatagramPacket(sendData, sendData.length, server.clientAddress,
                server.clientPort);
        server_socket.send(server.sendPacket);
    }
}
