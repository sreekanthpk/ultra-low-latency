package com.sree.examples.model;

public class RefData {
    private final byte[] instrumentName = new byte[4];
    public void set(byte[] instrumentName) {
        System.arraycopy(instrumentName, 0, this.instrumentName, 0, instrumentName.length);
    }

    public byte[] getInstrumentName() {
        return instrumentName;
    }
}
