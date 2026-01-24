package com.sree.examples;

import com.sree.examples.model.Tick;
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
        // Allocate once (important for latency)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(256);
        UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer);

        while (true) {
            for (byte[] sym : symbolBytes) {

                tick.set(sym, Math.random() * 500, (int) (Math.random() * 100));

                int offset = 0;

                // symbol (assume fixed length, e.g. 8 bytes)
                buffer.putBytes(offset, sym);
                offset += sym.length;

                // price
                buffer.putDouble(offset, tick.getPrice());
                offset += Double.BYTES;

                // size
                buffer.putInt(offset, tick.getSize());
                offset += Integer.BYTES;

                // publish
                while (publication.offer(buffer, 0, offset) < 0) {
                    Thread.yield();
                }
            }
        }
    }
}
