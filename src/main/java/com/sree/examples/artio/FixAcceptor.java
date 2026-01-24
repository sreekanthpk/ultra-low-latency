package com.sree.examples.artio;

import uk.co.real_logic.artio.library.AcquiringSessionExistsHandler;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.library.SessionHandler;
import uk.co.real_logic.artio.session.Session;

import static java.util.Collections.singletonList;

public class FixAcceptor {
    private static Session session;

    public static void main(String[] args) {
        final String aeronChannel = "aeron:udp?endpoint=localhost:10000";

        final LibraryConfiguration libraryConfiguration = new LibraryConfiguration();

        // You register the new session handler - which is your application hook
        // that receives messages for new sessions
        libraryConfiguration
                .sessionAcquireHandler((session, acquiredInfo) -> onConnect(session))
                .sessionExistsHandler(new AcquiringSessionExistsHandler())
                .libraryAeronChannels(singletonList(aeronChannel));


        FixLibrary library = FixLibrary.connect(libraryConfiguration);

        while (true) {
            library.poll(10);  // process events
        }
    }

    static SessionHandler onConnect(final Session session) {
        FixAcceptor.session = session;

        // Simple server just handles a single connection on a single thread
        // You choose how to manage threads for your application.

        return new FixAcceptorHandler(session);

    }
}