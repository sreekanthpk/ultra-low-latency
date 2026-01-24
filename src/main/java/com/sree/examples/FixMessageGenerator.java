package com.sree.examples;

import com.sree.examples.model.Tick;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class FixMessageGenerator {

    // 1. Pre-encoded constants to avoid String.getBytes() in the hot path
    private static final byte[] TAG_8 = "8=".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] VAL_FIX44 = "FIX.4.4|".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TAG_35 = "35=".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] VAL_MSG_TYPE = "6|".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TAG_55 = "55=".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TAG_270 = "270=".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TAG_271 = "271=".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TAG_48 = "48=".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TAG_10 = "10=".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] VAL_CSUM = "000|".getBytes(StandardCharsets.US_ASCII);

    private static final byte SOH = '|'; // Use (byte) 0x01 for production FIX

    private static final ThreadLocal<ByteBuffer> threadBuffer =
            ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(1024));

    public static ByteBuffer getBuffer() {
        ByteBuffer buffer = threadBuffer.get();
        buffer.clear();
        return buffer;
    }

    public static void writeFixMessage(Tick tick, ByteBuffer instrumentName, ByteBuffer buffer) {
        buffer.clear();

        // Standard Fields
        buffer.put(TAG_8).put(VAL_FIX44);
        buffer.put(TAG_35).put(VAL_MSG_TYPE);

        // Symbol (Assuming tick.getSymbol() returns a byte[])
        buffer.put(TAG_55);
        buffer.put(tick.getSymbol());
        buffer.put(SOH);

        // Price (Custom Zero-GC write)
        buffer.put(TAG_270);
        putDouble(buffer, tick.getPrice());
        buffer.put(SOH);

        // Size (Custom Zero-GC write)
        buffer.put(TAG_271);
        putLong(buffer, tick.getSize());
        buffer.put(SOH);

        if (instrumentName != null) {
            buffer.put(TAG_48);
            buffer.put(instrumentName);
            buffer.put(SOH);
        }

        buffer.put(TAG_10).put(VAL_CSUM);
        buffer.put((byte) '\n');

        buffer.flip();
    }

    /**
     * Efficiently writes a long to the ByteBuffer as ASCII without allocation.
     */
    public static void putLong(ByteBuffer buffer, long value) {
        if (value == 0) {
            buffer.put((byte) '0');
            return;
        }
        if (value < 0) {
            buffer.put((byte) '-');
            value = -value;
        }

        long p = 1;
        long tmp = value;
        while (tmp >= 10) {
            tmp /= 10;
            p *= 10;
        }
        while (p > 0) {
            buffer.put((byte) ('0' + (value / p)));
            value %= p;
            p /= 10;
        }
    }

    /**
     * Minimalist Zero-GC Double to ASCII.
     * For production, consider using a specialized library like Agrona's NumberUtil.
     */
    public static void putDouble(ByteBuffer buffer, double value) {
        // Simple implementation: write integer part, then decimal
        long longVal = (long) value;
        putLong(buffer, longVal);
        buffer.put((byte) '.');
        // Example: 4 decimal places
        long fraction = (long) ((value - longVal) * 10000);
        putLong(buffer, Math.abs(fraction));
    }
}