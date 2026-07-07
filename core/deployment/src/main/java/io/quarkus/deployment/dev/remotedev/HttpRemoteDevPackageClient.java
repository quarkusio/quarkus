package io.quarkus.deployment.dev.remotedev;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.runtime.util.HashUtil;

/**
 * Built-in HTTP/1.1 implementation of {@link RemoteDevPackageClient}.
 * <p>
 * One instance belongs to one build-tool remote-development session. Connection, delivery, and background polling
 * requests are serialized to preserve the endpoint protocol; delivery and polling share a monotonically increasing
 * session counter. The polling worker is a daemon thread owned by this client. It is interrupted during
 * {@link #close()} and, when close is invoked from another thread, joined with a bounded timeout.
 *
 * <p>
 * <strong>API note:</strong>
 * This class is public so Quarkus build-tool modules can construct the built-in client. It is implementation
 * API rather than a user-facing client SPI.
 * <p>
 * <strong>Implementation note:</strong>
 * The implementation uses fixed connection, request, restart, retry, and shutdown timeouts. Those values are
 * implementation policy and are not configurable through {@link RemoteDevPackageClientConfig}.
 */
public final class HttpRemoteDevPackageClient implements RemoteDevPackageClient {

    private static final Logger LOG = Logger.getLogger(HttpRemoteDevPackageClient.class);

    private static final String CONTENT_TYPE = "application/quarkus-live-reload";
    private static final String PASSWORD = "X-Quarkus-Password";
    private static final String SESSION = "X-Quarkus-Session";
    private static final String COUNT = "X-Quarkus-Count";
    private static final Duration RESTART_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration RESTART_INITIAL_DELAY = Duration.ofMillis(500);
    private static final Duration RESTART_RETRY_DELAY = Duration.ofMillis(100);
    private static final Duration POLL_FAILURE_DELAY = Duration.ofSeconds(1);
    private static final Duration POLL_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    private static final int RECONNECT_NOTIFICATION_ATTEMPTS = 3;

    private final RemoteDevPackageClientConfig config;
    private final HttpClient client;
    private final Duration restartTimeout;
    private final Duration restartInitialDelay;
    private final Duration restartRetryDelay;
    private final Duration pollFailureDelay;
    private final Duration pollCloseTimeout;
    /*
     * The remote endpoint rejects session counters that arrive out of order,
     * including the background /dev poll, so requestLock spans counter allocation
     * and the complete HTTP exchange. stateMonitor protects lifecycle/session/poller
     * state. Request code may briefly inspect state while holding requestLock, but
     * no state-monitor path acquires requestLock and reconnect callbacks run after
     * requestLock has been released.
     */
    private final ReentrantLock requestLock = new ReentrantLock(true);
    private final Object stateMonitor = new Object();
    private String session;
    private int count;
    private ClientState state = ClientState.NEW;
    private List<String> deferredDeletes = List.of();
    private Thread changePollingThread;

    /**
     * Creates a not-yet-connected client with the built-in timeout policy.
     *
     * @param config non-{@code null} endpoint and authentication configuration
     */
    public HttpRemoteDevPackageClient(RemoteDevPackageClientConfig config) {
        this(config, RESTART_TIMEOUT, RESTART_INITIAL_DELAY, RESTART_RETRY_DELAY, POLL_FAILURE_DELAY, POLL_CLOSE_TIMEOUT);
    }

    HttpRemoteDevPackageClient(RemoteDevPackageClientConfig config, Duration restartTimeout, Duration restartInitialDelay,
            Duration restartRetryDelay, Duration pollFailureDelay, Duration pollCloseTimeout) {
        this.config = config;
        this.restartTimeout = restartTimeout;
        this.restartInitialDelay = restartInitialDelay;
        this.restartRetryDelay = restartRetryDelay;
        this.pollFailureDelay = pollFailureDelay;
        this.pollCloseTimeout = pollCloseTimeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <strong>Implementation note:</strong>
     * A successful connection updates the session identifier when the endpoint supplies one and resets the
     * request counter. When the endpoint requests files, the client remains in reconciliation state until a
     * successful {@link #send(RemoteDevPackageDiff)}.
     */
    @Override
    public RemoteDevPackageClientResult connect(Map<String, String> localHashes) throws IOException {
        requestLock.lock();
        try {
            ensureNotClosed();
            byte[] body = remoteState(Map.copyOf(localHashes));
            HttpRequest.Builder request = request(resolve("/connect"))
                    .header("Content-Type", CONTENT_TYPE)
                    .header(PASSWORD, HashUtil.sha256(HashUtil.sha256(body) + password()))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            HttpResponse<String> response = send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("Remote dev connect to " + config.redactedRemoteUrl()
                        + " failed with status " + response.statusCode());
            }
            Set<String> requested = response.body() == null || response.body().isBlank()
                    ? Set.of()
                    : Arrays.stream(response.body().split(";"))
                            .filter(path -> !path.isBlank())
                            .collect(Collectors.toUnmodifiableSet());
            synchronized (stateMonitor) {
                ensureNotClosedLocked();
                session = response.headers().firstValue(SESSION).orElse(session);
                count = 0;
                state = requested.isEmpty() ? ClientState.CONNECTED : ClientState.RECONCILING;
                stateMonitor.notifyAll();
            }
            sendDeferredDeletes();
            return RemoteDevPackageClientResult.connected(requested);
        } finally {
            requestLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <strong>Implementation note:</strong>
     * The application-model file is delivered after the other changed files. Because it restarts the remote
     * application, deletions are deferred until the replacement session connects, when the old deployment
     * classpath is no longer open on Windows. A successful delivery of the application model waits for the endpoint
     * to become ready and returns {@link RemoteDevPackageClientOutcome#RECONNECT_REQUIRED}.
     */
    @Override
    public RemoteDevPackageClientResult send(RemoteDevPackageDiff diff) throws IOException {
        requestLock.lock();
        try {
            if (reconnectRequired()) {
                return RemoteDevPackageClientResult.reconnectRequired();
            }
            ensureSendable();
            try {
                sendDiff(diff);
            } catch (StaleSessionException e) {
                markReconnectRequired();
                return RemoteDevPackageClientResult.reconnectRequired();
            }
            if (diff.changed().stream().anyMatch(change -> RemoteDevPackageDiff.APPMODEL.equals(change.relativePath()))) {
                waitForRestart();
                markReconnectRequired();
                return RemoteDevPackageClientResult.reconnectRequired();
            }
            markReconciled();
            return RemoteDevPackageClientResult.sent(diff.changed().size(), diff.deleted().size());
        } finally {
            requestLock.unlock();
        }
    }

    private void sendDiff(RemoteDevPackageDiff diff) throws IOException {
        boolean restartsApplication = diff.changed().stream()
                .anyMatch(change -> RemoteDevPackageDiff.APPMODEL.equals(change.relativePath()));
        for (RemoteDevPackageChange change : diff.changed()) {
            if (!RemoteDevPackageDiff.APPMODEL.equals(change.relativePath())) {
                sendChange(change);
            }
        }
        if (!restartsApplication) {
            sendDeletes(diff.deleted());
        }
        // Updating the application model triggers a remote restart, so it follows the other changed files.
        // Deletions wait for the replacement session, after the old deployment classpath has been closed.
        for (RemoteDevPackageChange change : diff.changed()) {
            if (RemoteDevPackageDiff.APPMODEL.equals(change.relativePath())) {
                sendChange(change);
                deferredDeletes = diff.deleted();
            }
        }
    }

    private void sendDeferredDeletes() throws IOException {
        if (deferredDeletes.isEmpty()) {
            return;
        }
        sendDeletes(deferredDeletes);
        deferredDeletes = List.of();
    }

    private void sendDeletes(List<String> deleted) throws IOException {
        for (String path : deleted) {
            int requestCount = nextCount();
            String requestPath = "/" + path;
            HttpRequest request = request(resolve(requestPath))
                    .header("Content-Type", CONTENT_TYPE)
                    .header(SESSION, session())
                    .header(COUNT, Integer.toString(requestCount))
                    .header(PASSWORD,
                            HashUtil.sha256(HashUtil.sha256(requestPath.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                    + session() + requestCount + password()))
                    .DELETE()
                    .build();
            sendSuccessful(request, "delete " + path);
        }
    }

    private void sendChange(RemoteDevPackageChange change) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                change.file(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (attributes.isSymbolicLink() || !attributes.isRegularFile() || attributes.size() != change.size()) {
            throw changedAfterCapture(change);
        }
        byte[] bytes;
        try (var input = Files.newInputStream(change.file(), LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readAllBytes();
        }
        if (bytes.length != change.size() || !HashUtil.sha1(bytes).equals(change.sha1())) {
            throw changedAfterCapture(change);
        }
        int requestCount = nextCount();
        HttpRequest request = request(resolve("/" + change.relativePath()))
                .header("Content-Type", CONTENT_TYPE)
                .header(SESSION, session())
                .header(COUNT, Integer.toString(requestCount))
                .header(PASSWORD, HashUtil.sha256(HashUtil.sha256(bytes) + session() + requestCount + password()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        sendSuccessful(request, "upload " + change.relativePath());
    }

    private static IOException changedAfterCapture(RemoteDevPackageChange change) {
        return new IOException("Remote dev package file changed after snapshot capture: " + change.relativePath());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <strong>Implementation note:</strong>
     * Repeated calls while the polling thread is alive are idempotent. The reconnect listener runs on the
     * polling thread, outside the request lock.
     */
    @Override
    public void startChangePolling(RemoteDevPackageReconnectListener reconnectListener) throws IOException {
        Objects.requireNonNull(reconnectListener, "reconnectListener");
        synchronized (stateMonitor) {
            ensureConnectedLocked();
            if (changePollingThread != null && changePollingThread.isAlive()) {
                return;
            }
            changePollingThread = new Thread(() -> pollChanges(reconnectListener), "Quarkus remote dev change poller");
            changePollingThread.setDaemon(true);
            changePollingThread.start();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <strong>Implementation note:</strong>
     * Closing is state-idempotent. It interrupts the polling thread and waits for it with a bounded timeout;
     * the underlying JDK HTTP client has no close operation on the supported Java baseline.
     */
    @Override
    public void close() throws IOException {
        Thread thread;
        synchronized (stateMonitor) {
            if (state != ClientState.CLOSED) {
                state = ClientState.CLOSED;
                stateMonitor.notifyAll();
            }
            thread = changePollingThread;
        }
        if (thread != null) {
            thread.interrupt();
            if (thread != Thread.currentThread()) {
                joinPollingThread(thread);
            }
        }
    }

    private void pollChanges(RemoteDevPackageReconnectListener reconnectListener) {
        try {
            while (awaitConnected()) {
                try {
                    boolean pollFailed = false;
                    boolean stale = false;
                    requestLock.lockInterruptibly();
                    try {
                        if (!connected()) {
                            continue;
                        }
                        byte[] body = remoteProblem(null);
                        int requestCount = nextCount();
                        HttpRequest request = request(resolve("/dev"))
                                .header("Content-Type", CONTENT_TYPE)
                                .header(SESSION, session())
                                .header(COUNT, Integer.toString(requestCount))
                                .header(PASSWORD,
                                        HashUtil.sha256(HashUtil.sha256(body) + session() + requestCount + password()))
                                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                                .build();
                        HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
                        if (response.statusCode() == 203) {
                            LOG.debugf("Remote dev session for %s is no longer current", config.redactedRemoteUrl());
                            stale = markReconnectRequired();
                        } else if (response.statusCode() / 100 != 2) {
                            LOG.debugf("Remote dev change poll against %s failed with status %d",
                                    config.redactedRemoteUrl(),
                                    response.statusCode());
                            pollFailed = true;
                        }
                    } finally {
                        requestLock.unlock();
                    }
                    // Recovery can schedule a Gradle iteration and eventually call
                    // back into this client. Never invoke it while request ordering
                    // or state publication is locked.
                    if (stale && !notifyReconnectRequired(reconnectListener)) {
                        return;
                    }
                    if (pollFailed && !sleepAfterPollFailure()) {
                        return;
                    }
                } catch (IOException e) {
                    if (closed()) {
                        return;
                    }
                    LOG.debugf(e, "Remote dev change poll against %s failed", config.redactedRemoteUrl());
                    if (!sleepAfterPollFailure()) {
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (stateMonitor) {
                if (changePollingThread == Thread.currentThread()) {
                    changePollingThread = null;
                }
                stateMonitor.notifyAll();
            }
        }
    }

    private static byte[] remoteProblem(Throwable problem) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(problem);
        }
        return bytes.toByteArray();
    }

    private boolean sleepAfterPollFailure() {
        try {
            Thread.sleep(pollFailureDelay.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void sendSuccessful(HttpRequest request, String operation) throws IOException {
        HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 203) {
            throw new StaleSessionException();
        }
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Remote dev " + operation + " against " + config.redactedRemoteUrl()
                    + " failed with status " + response.statusCode());
        }
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return client.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while communicating with remote dev endpoint " + config.redactedRemoteUrl(), e);
        }
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(30));
    }

    private URI resolve(String path) {
        String base = config.remoteUrl().toString();
        if (base.endsWith("/") && path.startsWith("/")) {
            return URI.create(base.substring(0, base.length() - 1) + path);
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return URI.create(base + "/" + path);
        }
        return URI.create(base + path);
    }

    private String session() throws IOException {
        if (session == null) {
            throw new IOException("Remote dev session has not been established for " + config.redactedRemoteUrl());
        }
        return session;
    }

    private int nextCount() {
        return ++count;
    }

    private String password() throws IOException {
        return config.password()
                .orElseThrow(() -> new IOException("Remote dev password is required for " + config.redactedRemoteUrl()));
    }

    private static byte[] remoteState(Map<String, String> localHashes) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(new RemoteDevState(localHashes, null));
        }
        return bytes.toByteArray();
    }

    private void waitForRestart() throws IOException {
        sleep(restartInitialDelay);
        long deadline = System.nanoTime() + restartTimeout.toNanos();
        IOException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                HttpRequest probe = request(resolve("/probe"))
                        .header("Content-Type", CONTENT_TYPE)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<Void> response = send(probe, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() / 100 == 2) {
                    return;
                }
                lastFailure = new IOException("Remote dev restart probe against " + config.redactedRemoteUrl()
                        + " failed with status " + response.statusCode());
            } catch (IOException e) {
                lastFailure = e;
            }
            sleep(restartRetryDelay);
        }
        throw new IOException("Remote dev server at " + config.redactedRemoteUrl()
                + " did not become ready after an application-model restart", lastFailure);
    }

    private boolean notifyReconnectRequired(RemoteDevPackageReconnectListener reconnectListener) {
        for (int attempt = 1; attempt <= RECONNECT_NOTIFICATION_ATTEMPTS; attempt++) {
            if (!reconnectRequired()) {
                return true;
            }
            try {
                reconnectListener.reconnectRequired();
                return true;
            } catch (IOException | RuntimeException e) {
                if (attempt == RECONNECT_NOTIFICATION_ATTEMPTS) {
                    LOG.errorf(e,
                            "Unable to schedule automatic recovery for stale remote dev session at %s after %d attempts",
                            config.redactedRemoteUrl(), attempt);
                    return false;
                }
                LOG.debugf(e, "Unable to schedule automatic recovery for stale remote dev session at %s; retrying",
                        config.redactedRemoteUrl());
                if (!sleepAfterPollFailure()) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean awaitConnected() throws InterruptedException {
        synchronized (stateMonitor) {
            while (state == ClientState.RECONNECT_REQUIRED || state == ClientState.RECONCILING) {
                stateMonitor.wait();
            }
            return state == ClientState.CONNECTED;
        }
    }

    private boolean markReconnectRequired() {
        synchronized (stateMonitor) {
            if (state != ClientState.CONNECTED && state != ClientState.RECONCILING) {
                return false;
            }
            state = ClientState.RECONNECT_REQUIRED;
            return true;
        }
    }

    private void markReconciled() {
        synchronized (stateMonitor) {
            if (state == ClientState.RECONCILING) {
                state = ClientState.CONNECTED;
                stateMonitor.notifyAll();
            }
        }
    }

    private boolean reconnectRequired() {
        synchronized (stateMonitor) {
            return state == ClientState.RECONNECT_REQUIRED;
        }
    }

    private boolean connected() {
        synchronized (stateMonitor) {
            return state == ClientState.CONNECTED;
        }
    }

    private boolean closed() {
        synchronized (stateMonitor) {
            return state == ClientState.CLOSED;
        }
    }

    private void ensureSendable() throws IOException {
        synchronized (stateMonitor) {
            ensureNotClosedLocked();
            if (state != ClientState.CONNECTED && state != ClientState.RECONCILING) {
                throw new IOException("Remote dev session has not been established for " + config.redactedRemoteUrl());
            }
        }
    }

    private void ensureConnectedLocked() throws IOException {
        ensureNotClosedLocked();
        if (state != ClientState.CONNECTED) {
            throw new IOException("Remote dev session has not been established for " + config.redactedRemoteUrl());
        }
    }

    private void ensureNotClosed() throws IOException {
        synchronized (stateMonitor) {
            ensureNotClosedLocked();
        }
    }

    private void ensureNotClosedLocked() throws IOException {
        if (state == ClientState.CLOSED) {
            throw new IOException("Remote dev client for " + config.redactedRemoteUrl() + " is closed");
        }
    }

    private void joinPollingThread(Thread thread) throws IOException {
        try {
            long millis = pollCloseTimeout.toMillis();
            int nanos = (int) (pollCloseTimeout.minusMillis(millis).toNanos());
            thread.join(millis, nanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while stopping remote dev change polling for "
                    + config.redactedRemoteUrl(), e);
        }
        if (thread.isAlive()) {
            throw new IOException("Timed out stopping remote dev change polling for " + config.redactedRemoteUrl());
        }
    }

    private static void sleep(Duration duration) throws IOException {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for the remote dev server to restart", e);
        }
    }

    private static final class StaleSessionException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    private enum ClientState {
        NEW,
        RECONCILING,
        CONNECTED,
        RECONNECT_REQUIRED,
        CLOSED
    }
}
