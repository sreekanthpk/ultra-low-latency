# FixServer

A lightweight Java server that receives market ticks via Aeron,  generates FIX messages(uses quickfixj) via a Disruptor pipeline, and streams them to TCP clients. 

## Key features
\- Aeron subscriber for low\-latency market ticks  
\- Disruptor ring buffer for efficient event processing
\- Simple non\-blocking TCP server to stream FIX messages to clients

## Prerequisites
\- Java 21\+ (or the JDK version used by the project)  
\- Maven 3.6\+  
\- `pom.xml` configured with Aeron, Chronicle Map and Disruptor dependencies

## Build
Run from the project root:
\```bash
mvn clean package
\```

## Run
Run from the project root (example using the Maven exec plugin):
\```bash
mvn exec:java -XX:+UseZGC
-Xmx4g
-Xms4g
--illegal-access=permit
--add-exports
java.base/jdk.internal.ref=ALL-UNNAMED
--add-exports
java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens
java.base/java.lang=ALL-UNNAMED
--add-opens
java.base/sun.nio.ch=ALL-UNNAMED
--add-opens
java.base/java.nio=ALL-UNNAMED
--add-opens
java.base/java.util=ALL-UNNAMED
--add-opens
jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-opens
jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-opens
java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens
java.base/sun.misc=ALL-UNNAMED
-XX:StartFlightRecording=filename=app9.jfr,settings=profile,duration=30m,maxsize=2g,dumponexit=true,name=ProdRecording -Dexec.mainClass=FixServer
\```
Or run the generated classes/jar from the IDE (IntelliJ IDEA) or via:
\```bash
java -cp target/classes  -XX:+UseZGC
-Xmx4g
-Xms4g
--illegal-access=permit
--add-exports
java.base/jdk.internal.ref=ALL-UNNAMED
--add-exports
java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens
java.base/java.lang=ALL-UNNAMED
--add-opens
java.base/sun.nio.ch=ALL-UNNAMED
--add-opens
java.base/java.nio=ALL-UNNAMED
--add-opens
java.base/java.util=ALL-UNNAMED
--add-opens
jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-opens
jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-opens
java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens
java.base/sun.misc=ALL-UNNAMED
-XX:StartFlightRecording=filename=app9.jfr,settings=profile,duration=30m,maxsize=2g,dumponexit=true,name=ProdRecording FixServer
\```

## Configuration
The following values are configured in `src/main/java/FixServer.java` and can be externalized if needed:
\- TCP listen port: currently `5000` (passed to `runTcpServer`)  
\- Aeron endpoint: currently `aeron:udp?endpoint=localhost:40123`  
\- Aeron stream id: currently `1001`  
\- ChronicleMap sizes: `entries`, `averageKeySize`, `averageValueSize`

Adjust these constants in `FixServer` or externalize via environment variables or a properties file.

## Networking notes
\- The TCP server is non\-blocking and accepts one client socket (stored in a static field). Extend to support multiple clients by tracking clients in a threadsafe collection.  
\- `sendToClients` resets the `ByteBuffer` position before writing; ensure the buffer contains the correct limit/position for each send.

## Troubleshooting
\- If Aeron fails to bind, verify no other process is using the UDP port and that the Aeron native transport is compatible with your OS.  
\- If ChronicleMap allocation or growth is excessive, increase `averageKeySize`/`averageValueSize` and adjust `entries`.

## Project files
\- `README.md` (this file)  
\- `pom.xml` (Maven build and dependencies)  
\- `src/main/java/FixServer.java` (main application)

## License
Apache License, Version 2.0 (Apache-2.0)

## Profiling with Java Mission Control
Zero Colelction for 30 minutes run time. Uses ZGC

<img width="1918" height="1036" alt="image" src="https://github.com/user-attachments/assets/3df80af4-d7c5-4ad1-a00d-3683fe020c1c" />

No new Allocations for 30 mins. 

<img width="1917" height="1039" alt="image" src="https://github.com/user-attachments/assets/6724a11c-7981-4873-bc09-943eae9043cc" />

