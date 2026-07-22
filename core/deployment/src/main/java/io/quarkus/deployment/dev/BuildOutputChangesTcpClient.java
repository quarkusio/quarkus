package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.function.Function;

import org.jboss.logging.Logger;

final class BuildOutputChangesTcpClient implements Closeable {

    private static final Logger log = Logger.getLogger(BuildOutputChangesTcpClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final Socket socket;
    private volatile boolean closed;

    BuildOutputChangesTcpClient(InetSocketAddress address, String token,
            Function<BuildOutputChanges, BuildOutputChangesApplyStatus> consumer)
            throws IOException {
        requireNonNull(address, "address");
        requireNonNull(token, "token");
        requireNonNull(consumer, "consumer");
        var newSocket = new Socket();
        boolean success = false;
        try {
            newSocket.connect(address, (int) CONNECT_TIMEOUT.toMillis());
            BuildOutputChangesProtocol.writeHello(newSocket.getOutputStream(), token);
            newSocket.setSoTimeout(0);
            socket = newSocket;
            success = true;
        } finally {
            if (!success) {
                try {
                    newSocket.close();
                } catch (IOException e) {
                    log.debug("Failed to close external build output socket after connection failure", e);
                }
            }
        }
        var readThread = new Thread(() -> read(consumer), "Quarkus External Build Output TCP Client");
        readThread.setDaemon(true);
        readThread.start();
    }

    @Override
    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException e) {
            log.debug("Failed to close external build output socket", e);
        }
    }

    private void read(Function<BuildOutputChanges, BuildOutputChangesApplyStatus> consumer) {
        try (socket) {
            while (!closed && !socket.isClosed()) {
                String payload;
                try {
                    payload = BuildOutputChangesFrameCodec.read(socket.getInputStream());
                } catch (EOFException e) {
                    return;
                }
                BuildOutputChangesApplyStatus status;
                try {
                    BuildOutputChanges changes = BuildOutputChangesJsonCodec.decode(payload);
                    status = requireNonNull(consumer.apply(changes), "consumer result");
                } catch (RuntimeException e) {
                    log.warn("Failed to apply external build output message", e);
                    status = BuildOutputChangesApplyStatus.NOT_APPLIED;
                }
                BuildOutputChangesFrameCodec.write(socket.getOutputStream(), status.name());
            }
        } catch (SocketException e) {
            if (!closed) {
                log.debug("External build output TCP connection closed", e);
            }
        } catch (IOException | IllegalArgumentException e) {
            if (!closed) {
                log.warn("Dropping invalid external build output message", e);
            }
        }
    }
}
