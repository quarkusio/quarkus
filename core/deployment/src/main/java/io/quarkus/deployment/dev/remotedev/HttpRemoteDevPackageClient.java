package io.quarkus.deployment.dev.remotedev;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.runtime.util.HashUtil;

public final class HttpRemoteDevPackageClient implements RemoteDevPackageClient {

    private static final Logger LOG = Logger.getLogger(HttpRemoteDevPackageClient.class);

    private static final String CONTENT_TYPE = "application/quarkus-live-reload";
    private static final String PASSWORD = "X-Quarkus-Password";
    private static final String SESSION = "X-Quarkus-Session";
    private static final String COUNT = "X-Quarkus-Count";
    private static final Duration RESTART_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration RESTART_INITIAL_DELAY = Duration.ofMillis(500);
    private static final Duration RESTART_RETRY_DELAY = Duration.ofMillis(100);

    private final RemoteDevPackageClientConfig config;
    private final HttpClient client;
    private final Map<String, String> localHashes = new LinkedHashMap<>();
    private final Map<String, RemoteDevPackageChange> localFiles = new LinkedHashMap<>();
    // The remote endpoint rejects session counters that arrive out of order, including the background /dev poll.
    private final ReentrantLock requestLock = new ReentrantLock(true);
    private String session;
    private int count;
    private volatile boolean closed;
    private Thread changePollingThread;

    public HttpRemoteDevPackageClient(RemoteDevPackageClientConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public RemoteDevPackageClientResult connect(Map<String, String> localHashes) throws IOException {
        requestLock.lock();
        try {
            synchronized (this) {
                this.localHashes.clear();
                this.localHashes.putAll(localHashes);
                localFiles.clear();
            }
            return connectCurrentState();
        } finally {
            requestLock.unlock();
        }
    }

    private RemoteDevPackageClientResult connectCurrentState() throws IOException {
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
        synchronized (this) {
            session = response.headers().firstValue(SESSION).orElse(session);
            count = 0;
        }
        Set<String> requested = response.body() == null || response.body().isBlank()
                ? Set.of()
                : Arrays.stream(response.body().split(";"))
                        .filter(path -> !path.isBlank())
                        .collect(Collectors.toUnmodifiableSet());
        return RemoteDevPackageClientResult.connected(requested);
    }

    @Override
    public RemoteDevPackageClientResult send(RemoteDevPackageDiff diff) throws IOException {
        requestLock.lock();
        try {
            updateLocalState(diff);
            try {
                sendDiff(diff);
            } catch (StaleSessionException e) {
                reconnectAndUploadRequestedFiles();
            }
            if (diff.changed().stream().anyMatch(change -> RemoteDevPackageDiff.APPMODEL.equals(change.relativePath()))) {
                waitForRestartAndReconnect();
            }
            return RemoteDevPackageClientResult.sent(diff.changed().size(), diff.deleted().size());
        } finally {
            requestLock.unlock();
        }
    }

    private void sendDiff(RemoteDevPackageDiff diff) throws IOException {
        for (RemoteDevPackageChange change : diff.changed()) {
            if (!RemoteDevPackageDiff.APPMODEL.equals(change.relativePath())) {
                sendChange(change);
            }
        }
        for (String path : diff.deleted()) {
            int requestCount = nextCount();
            String requestPath = "/" + path;
            HttpRequest request = request(resolve("/" + path))
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
        // Updating the application model triggers a remote restart, so it must follow every other mutation.
        for (RemoteDevPackageChange change : diff.changed()) {
            if (RemoteDevPackageDiff.APPMODEL.equals(change.relativePath())) {
                sendChange(change);
            }
        }
    }

    private void sendChange(RemoteDevPackageChange change) throws IOException {
        byte[] bytes = Files.readAllBytes(change.file());
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

    @Override
    public synchronized void startChangePolling() throws IOException {
        session();
        if (changePollingThread != null && changePollingThread.isAlive()) {
            return;
        }
        closed = false;
        changePollingThread = new Thread(this::pollChanges, "Quarkus remote dev change poller");
        changePollingThread.setDaemon(true);
        changePollingThread.start();
    }

    @Override
    public void close() {
        closed = true;
        Thread thread;
        synchronized (this) {
            thread = changePollingThread;
            changePollingThread = null;
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void pollChanges() {
        while (!closed) {
            try {
                boolean pollFailed = false;
                requestLock.lockInterruptibly();
                try {
                    if (closed) {
                        return;
                    }
                    byte[] body = remoteProblem(null);
                    int requestCount = nextCount();
                    HttpRequest request = request(resolve("/dev"))
                            .header("Content-Type", CONTENT_TYPE)
                            .header(SESSION, session())
                            .header(COUNT, Integer.toString(requestCount))
                            .header(PASSWORD, HashUtil.sha256(HashUtil.sha256(body) + session() + requestCount + password()))
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                            .build();
                    HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
                    if (response.statusCode() == 203) {
                        LOG.debugf("Remote dev session for %s is no longer current", config.redactedRemoteUrl());
                        reconnectAndUploadRequestedFiles();
                        continue;
                    }
                    if (response.statusCode() / 100 != 2) {
                        LOG.debugf("Remote dev change poll against %s failed with status %d", config.redactedRemoteUrl(),
                                response.statusCode());
                        pollFailed = true;
                    }
                } finally {
                    requestLock.unlock();
                }
                if (pollFailed) {
                    sleepAfterPollFailure();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                if (!closed) {
                    LOG.debugf(e, "Remote dev change poll against %s failed", config.redactedRemoteUrl());
                    sleepAfterPollFailure();
                }
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

    private void sleepAfterPollFailure() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            closed = true;
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
        String currentSession;
        synchronized (this) {
            currentSession = session;
        }
        if (currentSession == null) {
            throw new IOException("Remote dev session has not been established for " + config.redactedRemoteUrl());
        }
        return currentSession;
    }

    private synchronized int nextCount() {
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

    private synchronized void updateLocalState(RemoteDevPackageDiff diff) {
        for (RemoteDevPackageChange change : diff.changed()) {
            localHashes.put(change.relativePath(), change.sha1());
            localFiles.put(change.relativePath(), change);
        }
        for (String deleted : diff.deleted()) {
            localHashes.remove(deleted);
            localFiles.remove(deleted);
        }
    }

    private synchronized void reconnectAndUploadRequestedFiles() throws IOException {
        RemoteDevPackageClientResult connected = connectCurrentState();
        if (connected.requestedPaths().isEmpty()) {
            return;
        }
        List<String> unknownPaths = connected.requestedPaths().stream()
                .filter(path -> !localFiles.containsKey(path))
                .sorted()
                .toList();
        if (!unknownPaths.isEmpty()) {
            throw new IOException("Remote dev reconnect to " + config.redactedRemoteUrl()
                    + " requested package files that are not available to the current client: " + unknownPaths);
        }
        List<RemoteDevPackageChange> requested = connected.requestedPaths().stream()
                .map(localFiles::get)
                .toList();
        sendDiff(new RemoteDevPackageDiff(requested, List.of()));
    }

    private void waitForRestartAndReconnect() throws IOException {
        sleep(RESTART_INITIAL_DELAY);
        long deadline = System.nanoTime() + RESTART_TIMEOUT.toNanos();
        IOException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                HttpRequest probe = request(resolve("/probe"))
                        .header("Content-Type", CONTENT_TYPE)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<Void> response = send(probe, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() / 100 == 2) {
                    reconnectAndUploadRequestedFiles();
                    return;
                }
                lastFailure = new IOException("Remote dev restart probe against " + config.redactedRemoteUrl()
                        + " failed with status " + response.statusCode());
            } catch (IOException e) {
                lastFailure = e;
            }
            sleep(RESTART_RETRY_DELAY);
        }
        throw new IOException("Remote dev server at " + config.redactedRemoteUrl()
                + " did not become ready after an application-model restart", lastFailure);
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
}
