package com.sree.examples;

import com.sree.examples.model.RefData;
import com.sree.examples.model.Tick;

import java.nio.ByteBuffer;

public interface FixAcceptor {
    void sendIOI(Tick tick, RefData refData);
    void start();
}
