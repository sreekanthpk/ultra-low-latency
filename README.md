# FixServer

A lightweight Java server that receives market ticks via Aeron,  generates FIX messages via a Disruptor pipeline, and streams them to TCP clients.

## Key features
\- Aeron subscriber for low\-latency market ticks  
\- Disruptor ring buffer for efficient event processing
\- Simple non\-blocking TCP server to stream FIX messages to clients

## Prerequisites
\- Java 17\+ (or the JDK version used by the project)  
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
mvn exec:java -Dexec.mainClass=FixServer
\```
Or run the generated classes/jar from the IDE (IntelliJ IDEA) or via:
\```bash
java -cp target/classes FixServer
\```

## Configuration
The following values are configured in `src/main/java/FixServer.java` and can be externalized if needed:
\- TCP listen port: currently `5000` (passed to `runTcpServer`)  
\- Aeron endpoint: currently `aeron:udp?endpoint=localhost:40123`  
\- Aeron stream id: currently `1001`  
\- ChronicleMap sizes: `entries`, `averageKeySize`, `averageValueSize`

Adjust these constants in `FixServer` or externalize via environment variables or a properties file.

## ChronicleMap & allocation tuning notes
\- The code uses a reusable buffer variable `instrumentValueBuffer` to avoid allocating a new `byte[]` on each lookup.  
\- `symbolMap.getUsing(key, instrumentValueBuffer)` copies the value into the provided buffer when available; check for `null` to detect missing keys.  
\- Set realistic values for `averageKeySize` and `averageValueSize` to avoid internal resizing and memory waste.  
\- For very large values or variable\-length payloads, consider storing references to off\-heap blobs or serializing into fixed\-size slices.

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
Specify your project license here (e.g., MIT, Apache\-2.0).
