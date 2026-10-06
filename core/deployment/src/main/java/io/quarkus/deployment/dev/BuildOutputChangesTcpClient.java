package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.function.Function;

import org.jboss.logging.Logger;

final class BuildOutputChangesTcpClient implements BuildOutputChangesConnection {

    private static final Logger log = Logger.getLogger(BuildOutputChangesTcpClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration WORKER_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    private final Socket socket;
    private final Function<BuildOutputChanges, BuildOutputChangesApplyStatus> consumer;
    /*
     * outputLock serializes complete frames from the reader and state-writer
     * threads. stateLock protects state coalescing and close notification. Neither
     * lock is nested inside the other: state is detached before a frame write, and
     * the potentially re-entrant consumer runs without either lock.
     */
    private final Object outputLock = new Object();
    private final Object stateLock = new Object();
    private final Thread readerThread;
    private final Thread stateWriterThread;
    private final Duration workerShutdownTimeout;
    private BuildOutputLiveReloadState pendingState;
    private long highestAcceptedGeneration = -1;
    private volatile boolean closed;

    BuildOutputChangesTcpClient(InetSocketAddress address, String token,
            Function<BuildOutputChanges, BuildOutputChangesApplyStatus> consumer)
            throws IOException {
        this(address, token, consumer, WORKER_SHUTDOWN_TIMEOUT);
    }

    BuildOutputChangesTcpClient(InetSocketAddress address, String token,
            Function<BuildOutputChanges, BuildOutputChangesApplyStatus> consumer, Duration workerShutdownTimeout)
            throws IOException {
        requireNonNull(address, "address");
        requireNonNull(token, "token");
        this.consumer = requireNonNull(consumer, "consumer");
        this.workerShutdownTimeout = requireNonNull(workerShutdownTimeout, "workerShutdownTimeout");
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
        stateWriterThread = daemonThread(this::writeStates, "Quarkus External Build Output State Writer");
        readerThread = daemonThread(this::readChanges, "Quarkus External Build Output TCP Client");
        stateWriterThread.start();
        readerThread.start();
    }

    @Override
    public void liveReloadStateChanged(BuildOutputLiveReloadState state) {
        requireNonNull(state, "state");
        synchronized (stateLock) {
            if (closed || state.generation() <= highestAcceptedGeneration) {
                return;
            }
            highestAcceptedGeneration = state.generation();
            pendingState = state;
            stateLock.notifyAll();
        }
    }

    @Override
    public void close() throws IOException {
        markClosed();
        IOException failure = null;
        try {
            socket.close();
        } catch (IOException e) {
            failure = e;
        }
        failure = awaitWorker(readerThread, failure);
        failure = awaitWorker(stateWriterThread, failure);
        if (failure != null) {
            throw failure;
        }
    }

    private void readChanges() {
        try {
            while (!closed && !socket.isClosed()) {
                BuildOutputChangesProtocol.Message message;
                try {
                    message = BuildOutputChangesProtocol.decode(
                            BuildOutputChangesFrameCodec.read(socket.getInputStream()));
                } catch (EOFException e) {
                    return;
                }
                if (!(message instanceof BuildOutputChangesProtocol.Changes changesMessage)) {
                    throw new IOException("Unexpected external build output message direction");
                }
                BuildOutputChangesApplyStatus status;
                try {
                    // Applying changes may enter the dev-mode scanner and publish a
                    // live-reload state update; keep it outside both client locks.
                    status = requireNonNull(consumer.apply(changesMessage.changes()), "consumer result");
                } catch (RuntimeException e) {
                    log.warn("Failed to apply external build output message", e);
                    status = BuildOutputChangesApplyStatus.NOT_APPLIED;
                }
                writeFrame(BuildOutputChangesProtocol.encodeApplyResult(changesMessage.requestId(), status));
            }
        } catch (SocketException e) {
            if (!closed) {
                log.debug("External build output TCP connection closed", e);
            }
        } catch (IOException e) {
            if (!closed) {
                log.warn("Dropping invalid external build output message", e);
            }
        } finally {
            markClosed();
            closeSocket();
        }
    }

    private void writeStates() {
        try {
            while (true) {
                BuildOutputLiveReloadState state;
                synchronized (stateLock) {
                    while (!closed && pendingState == null) {
                        try {
                            stateLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    if (closed) {
                        return;
                    }
                    state = pendingState;
                    pendingState = null;
                }
                writeFrame(BuildOutputChangesProtocol.encodeLiveReloadState(state));
            }
        } catch (SocketException e) {
            if (!closed) {
                log.debug("External build output TCP connection closed while publishing live-reload state", e);
            }
        } catch (IOException e) {
            if (!closed) {
                log.warn("Failed to publish external build output live-reload state", e);
            }
        } finally {
            markClosed();
            closeSocket();
        }
    }

    private void writeFrame(String payload) throws IOException {
        synchronized (outputLock) {
            if (closed) {
                throw new IOException("External build output TCP client is closed");
            }
            BuildOutputChangesFrameCodec.write(socket.getOutputStream(), payload);
        }
    }

    private void markClosed() {
        synchronized (stateLock) {
            closed = true;
            pendingState = null;
            stateLock.notifyAll();
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            log.debug("Failed to close external build output socket", e);
        }
    }

    private static Thread daemonThread(Runnable runnable, String name) {
        var thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }

    private IOException awaitWorker(Thread worker, IOException failure) {
        if (Thread.currentThread() == worker) {
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

    private static IOException addSuppressed(IOException failure, IOException additional) {
        if (failure == null) {
            return additional;
        }
        failure.addSuppressed(additional);
        return failure;
    }
}
