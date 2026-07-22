package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;

final class BuildOutputChangesTcpServer implements BuildOutputChangesServer {

    private static final Logger log = Logger.getLogger(BuildOutputChangesTcpServer.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration HELLO_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(300);

    private final ServerSocket serverSocket;
    private final String token;
    private final URI uri;
    private final CompletableFuture<Socket> authenticatedSocket = new CompletableFuture<>();
    private volatile Socket authenticatingSocket;
    private volatile Socket socket;
    private volatile boolean closed;

    BuildOutputChangesTcpServer() throws IOException {
        token = createToken();
        serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        uri = URI.create("tcp://127.0.0.1:" + serverSocket.getLocalPort());
        var acceptThread = new Thread(this::accept, "Quarkus External Build Output TCP Server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @Override
    public DevModeContext.ExternalBuildOutputTransport transport() {
        return DevModeContext.ExternalBuildOutputTransport.of(uri, token);
    }

    @Override
    public BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws IOException {
        requireNonNull(changes, "changes");
        @SuppressWarnings("resource")
        var socket = connectedSocket();
        synchronized (socket) {
            BuildOutputChangesFrameCodec.write(socket.getOutputStream(), BuildOutputChangesJsonCodec.encode(changes));
            int previousTimeout = socket.getSoTimeout();
            socket.setSoTimeout((int) RESPONSE_TIMEOUT.toMillis());
            try {
                return readApplyStatus(socket);
            } finally {
                socket.setSoTimeout(previousTimeout);
            }
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        IOException failure = null;
        try {
            serverSocket.close();
        } catch (IOException e) {
            failure = e;
        }
        if (socket == null) {
            authenticatedSocket.completeExceptionally(new IOException("External build output TCP server is closed"));
        } else {
            failure = closeSocket(socket, failure);
        }
        if (authenticatingSocket != null) {
            failure = closeSocket(authenticatingSocket, failure);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private Socket connectedSocket() throws IOException {
        try {
            return authenticatedSocket.get(CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted waiting for external build output TCP client", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("External build output TCP client failed", cause);
        } catch (TimeoutException e) {
            throw new IOException("Timed out waiting for external build output TCP client", e);
        }
    }

    private static BuildOutputChangesApplyStatus readApplyStatus(Socket socket) throws IOException {
        String payload = BuildOutputChangesFrameCodec.read(socket.getInputStream());
        try {
            return BuildOutputChangesApplyStatus.valueOf(payload);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid external build output apply status: " + payload, e);
        }
    }

    private void accept() {
        while (!closed && !serverSocket.isClosed() && !authenticatedSocket.isDone()) {
            Socket acceptedSocket = null;
            try {
                acceptedSocket = serverSocket.accept();
                authenticatingSocket = acceptedSocket;
                acceptedSocket.setSoTimeout((int) HELLO_TIMEOUT.toMillis());
                String receivedToken = BuildOutputChangesProtocol.readHello(acceptedSocket.getInputStream());
                if (!sameToken(token, receivedToken)) {
                    acceptedSocket.close();
                    acceptedSocket = null;
                    continue;
                }
                acceptedSocket.setSoTimeout(0);
                socket = acceptedSocket;
                authenticatedSocket.complete(acceptedSocket);
                acceptedSocket = null;
            } catch (SocketException e) {
                if (!closed) {
                    authenticatedSocket.completeExceptionally(e);
                    log.debug("External build output TCP server socket closed", e);
                }
            } catch (IOException e) {
                if (!closed) {
                    log.debug("Rejected external build output TCP client", e);
                }
            } finally {
                authenticatingSocket = null;
                if (acceptedSocket != null) {
                    try {
                        acceptedSocket.close();
                    } catch (IOException e) {
                        log.debug("Failed to close rejected external build output TCP client", e);
                    }
                }
            }
        }
    }

    private static IOException closeSocket(Socket socket, IOException failure) {
        try {
            socket.close();
            return failure;
        } catch (IOException e) {
            if (failure == null) {
                return e;
            }
            failure.addSuppressed(e);
            return failure;
        }
    }

    private static String createToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean sameToken(String expected, String received) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
    }
}
