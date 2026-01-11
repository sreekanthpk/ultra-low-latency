package com.sree.examples;

public final class Tick {
    private final byte[] symbol = new byte[4]; // fixed-length symbol
    private double price;
    private int size;

    public void set(byte[] sym, double price, int size) {
        System.arraycopy(sym, 0, this.symbol, 0, sym.length);
        this.price = price;
        this.size = size;
    }

    public byte[] getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getSize() { return size; }

    @Override
    public String toString() {
        return "Tick{" + "symbol=" + new String(symbol).trim() +
                ", price=" + price +
                ", size=" + size + '}';
    }
}
