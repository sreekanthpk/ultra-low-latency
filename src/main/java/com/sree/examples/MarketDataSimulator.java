package com.sree.examples;

import io.aeron.Aeron;
import io.aeron.Publication;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public final class MarketDataSimulator {

    public static void main(String[] args) {
        Aeron.Context ctx = new Aeron.Context();
        Aeron aeron = Aeron.connect(ctx);
        Publication publication = aeron.addPublication("aeron:udp?endpoint=localhost:40123", 1001);

        String[] symbols = {"AAPL","GOOG","MSFT"};
        byte[][] symbolBytes = new byte[symbols.length][4];
        for(int i=0;i<symbols.length;i++){
            System.arraycopy(symbols[i].getBytes(),0,symbolBytes[i],0,4);
        }

        Tick tick = new Tick();
        ByteBuffer buffer = ByteBuffer.allocateDirect(256);
        DirectBuffer dBuffer= new UnsafeBuffer(buffer);

        while (true) {
            for (byte[] sym : symbolBytes) {
                tick.set(sym, Math.random() * 500, (int)(Math.random() * 100));
                buffer.clear();
                buffer.put(sym);
                buffer.putDouble(tick.getPrice());
                buffer.putInt(tick.getSize());
                buffer.flip();
                while (publication.offer(dBuffer) < 0L) {
                    Thread.yield();
                }
            }
        }
    }
}
