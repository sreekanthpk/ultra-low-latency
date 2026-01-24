package com.sree.examples;

import com.sree.examples.qucikfix4j.QFJFixAcceptor;

public class FixEngineFactory {

    private static FixAcceptor QFIX4J_INSTANCE = new QFJFixAcceptor();
    public static FixAcceptor getFixAcceptor() {
        return QFIX4J_INSTANCE;
    }
}
