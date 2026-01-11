package com.sree.examples;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public final class FixClient {

    private final SocketChannel channel;
    private final ByteBuffer writeBuffer;
    private final ByteBuffer readBuffer;

    public FixClient(String host, int port, int bufferSize) throws IOException {
        this.channel = SocketChannel.open();
        this.channel.configureBlocking(true); // blocking for simplicity
        this.channel.connect(new InetSocketAddress(host, port));

        // Reuse buffers for zero-GC
        this.writeBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.readBuffer = ByteBuffer.allocateDirect(bufferSize);

        System.out.println("Connected to FIX server at " + host + ":" + port);
    }

    // =====================
    // Send message zero-GC
    // =====================
    public void send(ByteBuffer message) throws IOException {
        writeBuffer.clear();
        writeBuffer.put(message);
        writeBuffer.flip();
        while (writeBuffer.hasRemaining()) {
            channel.write(writeBuffer);
        }
    }

    // =====================
    // Receive messages zero-GC
    // =====================
    public void receiveLoop() throws IOException {
        while (true) {
            readBuffer.clear();
            int bytesRead = channel.read(readBuffer);
            if (bytesRead == -1) {
                System.out.println("Server closed connection.");
                break;
            } else if (bytesRead > 0) {
                readBuffer.flip();
                byte[] tmp = new byte[readBuffer.remaining()];
                readBuffer.get(tmp);

                // Convert bytes to ASCII string and replace SOH with |
                String fixMsg = new String(tmp, java.nio.charset.StandardCharsets.US_ASCII);

                System.out.println("Received FIX: " + fixMsg);

                readBuffer.clear();
            }
        }
    }

    public void close() throws IOException {
        channel.close();
    }

    // =======================
    // Main method demo
    // =======================
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            FixClient client = new FixClient("127.0.0.1", 5000, 1024);

            // Pre-allocated buffer for sending
            ByteBuffer buffer = FixMessageGenerator.getBuffer();

            // Start a thread to receive server messages
            Thread receiver = new Thread(() -> {
                try {
                    client.receiveLoop();
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            });
            receiver.start();

            System.out.println("Type 'quit' to exit. Enter dummy FIX messages to send:");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("quit".equalsIgnoreCase(input)) break;

                buffer.clear();
                buffer.put(input.getBytes()); // can be optimized to pre-encoded bytes for zero-GC
                buffer.put((byte) '\n');
                buffer.flip();

                client.send(buffer);
            }

            client.close();
            System.out.println("Client exited.");

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
