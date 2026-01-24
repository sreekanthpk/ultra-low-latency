package com.sree.examples.artio;

import io.aeron.driver.MediaDriver;

import org.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.artio.Reply;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.library.SessionConfiguration;
import uk.co.real_logic.artio.session.Session;

import java.io.File;

import static com.sree.examples.artio.FixAcceptor.onConnect;
import static io.aeron.driver.ThreadingMode.SHARED;
import static java.util.Collections.singletonList;
import static uk.co.real_logic.artio.CommonConfiguration.optimalTmpDirName;

public class FixInitiator {

    public static void main(String[] args) {
        final String aeronChannel = "aeron:udp?endpoint=localhost:10002";
        final EngineConfiguration configuration = new EngineConfiguration()
                .libraryAeronChannel(aeronChannel)
                .monitoringFile(optimalTmpDirName() + File.separator + "fix-client" + File.separator + "engineCounters")
                .logFileDir("client-logs");


        final MediaDriver.Context context = new MediaDriver.Context()
                .threadingMode(SHARED)
                .dirDeleteOnStart(true);

        final SessionConfiguration sessionConfig = SessionConfiguration.builder()
                .address("localhost", 9999)
                .targetCompId("CLIENT")
                .senderCompId("SERVER")
                .build();

        final SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy(100);

        final LibraryConfiguration libraryConfiguration = new LibraryConfiguration()
                .sessionAcquireHandler((session, acquiredInfo) -> onConnect(session))
                .libraryAeronChannels(singletonList(aeronChannel));
        // Connect library to an engine (maybe remote)
        FixLibrary library = FixLibrary.connect(libraryConfiguration);

        // Initiate connection
        Reply<Session> reply =
                library.initiate(sessionConfig);

        // Wait status & check success
        if (reply.hasCompleted()) {
            System.out.println("Initiator session established!");
        } else {
            System.err.println("Initiator failed: " + reply.error());
        }

        // Poll the library to process messages
        while (true) {
            library.poll(10);
        }
    }
}
