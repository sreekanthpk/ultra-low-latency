package com.sree.examples.qucikfix4j;

import quickfix.*;
import quickfix.field.*;

import java.util.concurrent.CountDownLatch;

public class FixInitiator extends MessageCracker implements Application {

    @Override
    public void onCreate(SessionID sessionId) {

    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Logged on to server");
    }

    @Override
    public void onLogout(SessionID sessionId) {}

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        System.out.println("toAdmin  message: " + message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        System.out.println("from admin message: " + message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        System.out.println("to app message: " + message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        crack(message, sessionId);
    }

    @Override
    public void onMessage(Message ioi, SessionID sessionID) {
        System.out.println("IndicationOfInterest received" + ioi.toString());
    }


    public static void main(String[] args) throws Exception {
        SessionSettings settings = new SessionSettings("Initiator.cfg");
        Application app = new FixInitiator();
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();

        Initiator initiator =
                new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);

        initiator.start();
        System.out.println("FIX Initiator started...");
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}