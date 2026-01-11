package com.sree.examples;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.driver.ThreadingMode;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;

import org.agrona.concurrent.BackoffIdleStrategy;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import java.util.Iterator;

import java.util.concurrent.Executors;

public final class FixServer {

    private static SocketChannel client;

    public static byte[] instrumentNameBuffer = new byte[4];

    public static void main(String[] args)  {

        MediaDriver.Context ctx = new MediaDriver.Context()
                .dirDeleteOnStart(true)
                .warnIfDirectoryExists(true)
                .threadingMode(ThreadingMode.DEDICATED);
        MediaDriver driver = MediaDriver.launch(ctx);
        System.out.println("MediaDriver started at " + driver.aeronDirectoryName());

        // Chronicle Map (off-heap) -- Not used due to new gen allocation
        /*ChronicleMap<Bytes, Bytes> symbolMap = ChronicleMapBuilder
                .of(Bytes.class, Bytes.class)
                .name("raw-byte-map")
                .entries(100_000)
                .averageKeySize(32)
                .averageValueSize(256)
                .create();

        // Pre-allocate reusable key/value Bytes objects
        Bytes key = Bytes.allocateDirect(32);
        Bytes value = Bytes.allocateDirect(256);

        // Populate map
        key.clear().append("AAPL"); value.clear().append("Apple"); symbolMap.put(key, value);
        key.clear().append("GOOG"); value.clear().append("Alphabet"); symbolMap.put(key, value);
        key.clear().append("MSFT"); value.clear().append("Microsoft"); symbolMap.put(key, value);*/

        // Start TCP server to accept clients
        new Thread(() -> runTcpServer(5000)).start();

        // Disruptor for processing ticks
        RingBuffer<TickEvent> ringBuffer = RingBuffer.create(
                ProducerType.SINGLE,
                TickEvent::new,
                1024,
                new BusySpinWaitStrategy()
        );

        EventHandler<TickEvent> handler = (event, sequence, endOfBatch) -> {
            Tick tick = event.getTick();
            ByteBuffer buffer = FixMessageGenerator.getBuffer();
            FixMessageGenerator.writeFixMessage(tick, null , buffer);
            sendToClients(buffer);
        };

        BatchEventProcessor<TickEvent> processor =
                new BatchEventProcessor<>(ringBuffer, ringBuffer.newBarrier(), handler);
        ringBuffer.addGatingSequences(processor.getSequence());
        Executors.newSingleThreadExecutor().submit(processor);

        // Aeron subscriber for market data ticks
        Aeron aeron = Aeron.connect();
        Subscription subscription = aeron.addSubscription("aeron:udp?endpoint=localhost:40123", 1001);
        byte[] sym = new byte[4];

        FragmentHandler fragmentHandler = (buffer, offset, length, header) -> {
            buffer.getBytes(offset, sym);
            offset += 4;
            double price = buffer.getDouble(offset);
            offset += 8;
            int size = buffer.getInt(offset);

            long seq = ringBuffer.next();
            try {
                TickEvent event = ringBuffer.get(seq);
                event.getTick().set(sym, price, size);
            } finally {
                ringBuffer.publish(seq);
            }
        };

        BackoffIdleStrategy idleStrategy = new BackoffIdleStrategy(1, 10, 1, 10);

        // Main loop: poll Aeron for ticks
        while (true) {
            int fragments = subscription.poll(fragmentHandler, 10);
            if (fragments == 0) idleStrategy.idle(0);
        }
    }

    private static void runTcpServer(int port) {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("TCP server started on port " + port);

            while (true) {
                selector.select(10);
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        client = serverChannel.accept();
                        client.configureBlocking(false);

                        System.out.println("Client connected: " + client.getRemoteAddress());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void sendToClients(ByteBuffer message) {
        if(null== client) return;
        // Reset position for each client without duplicating the object
        message.position(0);
        while(message.hasRemaining()) {
            try {
                client.write(message);
            } catch (IOException e) {
                System.out.println("Error writing to client: " + e.getMessage());
            }
        }

    }
}
