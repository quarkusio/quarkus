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
import java.util.function.Consumer;

import org.jboss.logging.Logger;

final class BuildOutputChangesTcpServer implements BuildOutputChangesServer {

    private static final Logger log = Logger.getLogger(BuildOutputChangesTcpServer.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration HELLO_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(300);
    private static final Duration WORKER_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    private final ServerSocket serverSocket;
    private final String token;
    private final URI uri;
    private final Consumer<BuildOutputLiveReloadState> stateListener;
    private final Duration responseTimeout;
    private final Duration workerShutdownTimeout;
    private final CompletableFuture<Socket> authenticatedSocket = new CompletableFuture<>();
    private final CompletableFuture<Void> termination = new CompletableFuture<>();
    /*
     * sendLock permits one request/response exchange. connectionLock protects
     * request correlation, connection-wide sequence state, and shutdown.
     * callbackLock only protects the coalesced state notification. The only nested
     * acquisition is sendLock -> connectionLock; futures and user callbacks are
     * completed/invoked after releasing connectionLock and callbackLock.
     */
    private final Object sendLock = new Object();
    private final Object connectionLock = new Object();
    private final Object callbackLock = new Object();
    private final Thread acceptThread;
    private final Thread callbackThread;
    private volatile Thread receiverThread;
    private volatile Socket authenticatingSocket;
    private volatile Socket socket;
    private PendingRequest pendingRequest;
    private BuildOutputLiveReloadState pendingCallback;
    private long latestStateGeneration = -1;
    private long nextRequestId;
    private boolean requestIdsExhausted;
    private volatile boolean closed;

    BuildOutputChangesTcpServer() throws IOException {
        this(ignored -> {
        });
    }

    BuildOutputChangesTcpServer(Consumer<BuildOutputLiveReloadState> stateListener) throws IOException {
        this(stateListener, RESPONSE_TIMEOUT, WORKER_SHUTDOWN_TIMEOUT, 0);
    }

    BuildOutputChangesTcpServer(Consumer<BuildOutputLiveReloadState> stateListener, Duration responseTimeout,
            Duration workerShutdownTimeout, long initialRequestId) throws IOException {
        this.stateListener = requireNonNull(stateListener, "stateListener");
        this.responseTimeout = requireNonNull(responseTimeout, "responseTimeout");
        this.workerShutdownTimeout = requireNonNull(workerShutdownTimeout, "workerShutdownTimeout");
        if (initialRequestId < 0) {
            throw new IllegalArgumentException("Initial request ID must not be negative");
        }
        nextRequestId = initialRequestId;
        token = createToken();
        serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        uri = URI.create("tcp://127.0.0.1:" + serverSocket.getLocalPort());
        callbackThread = daemonThread(this::dispatchStates, "Quarkus External Build Output State Dispatcher");
        acceptThread = daemonThread(this::accept, "Quarkus External Build Output TCP Server");
        callbackThread.start();
        acceptThread.start();
    }

    @Override
    public DevModeContext.ExternalBuildOutputTransport transport() {
        return DevModeContext.ExternalBuildOutputTransport.of(uri, token);
    }

    @Override
    public BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws IOException {
        requireNonNull(changes, "changes");
        synchronized (sendLock) {
            Socket connected = connectedSocket();
            PendingRequest request;
            synchronized (connectionLock) {
                assertOpen();
                if (pendingRequest != null) {
                    throw new IOException("External build output request is already in flight");
                }
                if (requestIdsExhausted) {
                    request = null;
                } else {
                    long requestId = nextRequestId;
                    if (requestId == Long.MAX_VALUE) {
                        requestIdsExhausted = true;
                    } else {
                        nextRequestId = Math.incrementExact(requestId);
                    }
                    request = new PendingRequest(requestId, new CompletableFuture<>());
                    pendingRequest = request;
                }
            }
            if (request == null) {
                IOException failure = new IOException("External build output request IDs are exhausted");
                shutdown(failure, false);
                throw failure;
            }
            try {
                BuildOutputChangesFrameCodec.write(connected.getOutputStream(),
                        BuildOutputChangesProtocol.encodeChanges(request.requestId(), changes));
            } catch (IOException e) {
                shutdown(e, false);
                throw e;
            }
            return awaitResult(request);
        }
    }

    @Override
    public CompletableFuture<Void> termination() {
        return termination;
    }

    @Override
    public void close() throws IOException {
        IOException failure = shutdown(new IOException("External build output TCP server is closed"), true);
        failure = awaitWorker(acceptThread, failure);
        failure = awaitWorker(receiverThread, failure);
        failure = awaitWorker(callbackThread, failure);
        if (failure != null) {
            throw failure;
        }
    }

    private BuildOutputChangesApplyStatus awaitResult(PendingRequest request) throws IOException {
        try {
            return request.result().get(responseTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            IOException failure = new IOException("Interrupted waiting for external build output apply result", e);
            shutdown(failure, false);
            throw failure;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("External build output apply failed", cause);
        } catch (TimeoutException e) {
            IOException failure = new IOException("Timed out waiting for external build output apply result", e);
            shutdown(failure, false);
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
            IOException failure = new IOException("Timed out waiting for external build output TCP client", e);
            shutdown(failure, false);
            throw failure;
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
                receiverThread = daemonThread(this::receive, "Quarkus External Build Output TCP Receiver");
                receiverThread.start();
                acceptedSocket = null;
            } catch (SocketException e) {
                if (!closed) {
                    shutdown(e, false);
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

    private void receive() {
        try {
            while (!closed) {
                BuildOutputChangesProtocol.Message message = BuildOutputChangesProtocol.decode(
                        BuildOutputChangesFrameCodec.read(socket.getInputStream()));
                if (message instanceof BuildOutputChangesProtocol.ApplyResult applyResult) {
                    receiveApplyResult(applyResult);
                } else if (message instanceof BuildOutputChangesProtocol.LiveReloadState liveReloadState) {
                    receiveLiveReloadState(liveReloadState.state());
                } else {
                    throw new IOException("Unexpected external build output message direction");
                }
            }
        } catch (SocketException e) {
            if (!closed) {
                shutdown(e, false);
                log.debug("External build output TCP connection closed", e);
            }
        } catch (IOException e) {
            if (!closed) {
                shutdown(e, false);
                log.warn("Dropping invalid external build output TCP connection", e);
            }
        }
    }

    private void receiveApplyResult(BuildOutputChangesProtocol.ApplyResult result) throws IOException {
        CompletableFuture<BuildOutputChangesApplyStatus> completion;
        synchronized (connectionLock) {
            if (pendingRequest == null || pendingRequest.requestId() != result.requestId()) {
                throw new IOException("Unknown external build output request ID");
            }
            completion = pendingRequest.result();
            pendingRequest = null;
        }
        // Completion wakes the sender and may run dependent callbacks. Do it after
        // releasing connectionLock to keep callback code outside transport state.
        if (!completion.complete(result.status())) {
            throw new IOException("External build output request completed more than once");
        }
    }

    private void receiveLiveReloadState(BuildOutputLiveReloadState state) {
        synchronized (connectionLock) {
            if (state.generation() <= latestStateGeneration) {
                return;
            }
            latestStateGeneration = state.generation();
        }
        synchronized (callbackLock) {
            if (closed) {
                return;
            }
            if (pendingCallback == null || state.generation() > pendingCallback.generation()) {
                pendingCallback = state;
                callbackLock.notifyAll();
            }
        }
    }

    private void dispatchStates() {
        while (true) {
            BuildOutputLiveReloadState state;
            synchronized (callbackLock) {
                while (!closed && pendingCallback == null) {
                    try {
                        callbackLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (closed) {
                    return;
                }
                state = pendingCallback;
                pendingCallback = null;
            }
            // Listener code can request a Gradle replay and may re-enter unrelated
            // session state, so it must never run under a transport lock.
            try {
                stateListener.accept(state);
            } catch (RuntimeException e) {
                log.warn("External build output live-reload state listener failed", e);
            }
        }
    }

    private IOException shutdown(IOException cause, boolean expected) {
        Socket connected;
        Socket authenticating;
        PendingRequest request;
        synchronized (connectionLock) {
            if (closed) {
                return null;
            }
            closed = true;
            connected = socket;
            authenticating = authenticatingSocket;
            request = pendingRequest;
            pendingRequest = null;
        }
        // Closing sockets and completing waiters can wake arbitrary peer/session
        // code. The closed state and detached request are published first.
        authenticatedSocket.completeExceptionally(cause);
        if (request != null) {
            request.result().completeExceptionally(cause);
        }
        synchronized (callbackLock) {
            pendingCallback = null;
            callbackLock.notifyAll();
        }
        IOException failure = null;
        failure = closeSocket(serverSocket, failure);
        failure = closeSocket(connected, failure);
        if (authenticating != connected) {
            failure = closeSocket(authenticating, failure);
        }
        if (expected) {
            termination.complete(null);
        } else {
            termination.completeExceptionally(
                    new IOException("External build output transport terminated unexpectedly"));
        }
        return failure;
    }

    private void assertOpen() throws IOException {
        if (closed) {
            throw new IOException("External build output TCP server is closed");
        }
    }

    private IOException awaitWorker(Thread worker, IOException failure) {
        if (worker == null || Thread.currentThread() == worker) {
            return failure;
        }
        try {
            worker.join(workerShutdownTimeout.toMillis());
            if (worker.isAlive()) {
                failure = addSuppressed(failure,
                        new IOException("Timed out stopping external build output worker " + worker.getName()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failure = addSuppressed(failure,
                    new IOException("Interrupted stopping external build output worker " + worker.getName(), e));
        }
        return failure;
    }

    private static IOException closeSocket(AutoCloseable closeable, IOException failure) {
        if (closeable == null) {
            return failure;
        }
        try {
            closeable.close();
            return failure;
        } catch (Exception e) {
            IOException ioException = e instanceof IOException ioe ? ioe : new IOException(e);
            return addSuppressed(failure, ioException);
        }
    }

    private static IOException addSuppressed(IOException failure, IOException additional) {
        if (failure == null) {
            return additional;
        }
        failure.addSuppressed(additional);
        return failure;
    }

    private static Thread daemonThread(Runnable runnable, String name) {
        var thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
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

    private record PendingRequest(
            long requestId,
            CompletableFuture<BuildOutputChangesApplyStatus> result) {
    }
}
