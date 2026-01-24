package com.sree.examples.qucikfix4j;

import com.sree.examples.FixAcceptor;
import com.sree.examples.model.RefData;
import com.sree.examples.model.Tick;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.IndicationOfInterest;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class QFJFixAcceptor extends MessageCracker implements Application, FixAcceptor {

    private SessionID sessionId;
    private Queue<IndicationOfInterest> indicationOfInterestPool =  new LinkedList<IndicationOfInterest>();
    private volatile int sequencer = 1;

    @Override
    public void onCreate(SessionID sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Client logged on: " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Client logged out: " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        System.out.println("to admin"+ message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        System.out.println("to admin"+ message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        System.out.println("to app"+ message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("from app"+ message);
        crack(message, sessionId);
    }

    public void sendIOI(Tick tick, RefData refData)  {

        IndicationOfInterest ioi = indicationOfInterestPool.poll();

        ioi.set(new IOIID("IOI-"+sequencer++));
        ioi.set(new IOITransType(IOITransType.NEW));   // N
        ioi.set(new Symbol(new String(tick.getSymbol(), StandardCharsets.UTF_8)));
        ioi.set(new Side(Side.BUY));
        ioi.set(new Price(tick.getPrice()));
        ioi.set(new OrderQty(tick.getSize()*1.0));
        ioi.set(new IOIQty(IOIQty.MEDIUM));

        try {
            Session.sendToTarget(ioi, this.sessionId);
        } catch (SessionNotFound e) {
            System.out.println("Error sending IOI: " + e.getMessage());
        }
        indicationOfInterestPool.add(ioi);
    }

    public void start()  {
        SessionSettings settings = null;
        try {
            settings = new SessionSettings("Acceptor.cfg");
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new FileLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();

            Acceptor acceptor =
                    new SocketAcceptor(this, storeFactory, settings, logFactory, messageFactory);
            for(int i=0;i<100;i++){
                indicationOfInterestPool.add(new IndicationOfInterest());
            }
            acceptor.start();
            System.out.println("FIX Acceptor started...");
        } catch (ConfigError e) {
            throw new RuntimeException(e);
        }
    }
}