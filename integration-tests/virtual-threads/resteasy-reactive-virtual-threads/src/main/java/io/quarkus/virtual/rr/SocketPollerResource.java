package io.quarkus.virtual.rr;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.runtime.ImageMode;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;

/**
 * Blocks a virtual thread on a JDK socket, which starts the JDK's pollers, and reports their thread names.
 */
@Path("/socket-pollers")
public class SocketPollerResource {

    @GET
    @RunOnVirtualThread
    public String pollers() throws IOException {
        VirtualThreadsAssertions.assertEverything();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
                Socket client = new Socket(server.getInetAddress(), server.getLocalPort());
                Socket accepted = server.accept()) {
            // nothing to read for 200 ms: the virtual thread waits for a poller, then the read times out
            client.setSoTimeout(200);
            try {
                client.getInputStream().read();
            } catch (SocketTimeoutException expected) {
                // the pollers are running now
            }
        }
        String mode = ImageMode.current() == ImageMode.NATIVE_RUN ? "native" : "jvm";
        return mode + ":" + Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getName)
                .filter(name -> name.endsWith("Poller"))
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }
}
