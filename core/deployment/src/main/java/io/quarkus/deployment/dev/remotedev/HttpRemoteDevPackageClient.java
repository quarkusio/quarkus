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
import java.util.Map;
import java.util.Set;
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

    private final RemoteDevPackageClientConfig config;
    private final HttpClient client;
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
        byte[] body = remoteState(localHashes);
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
        int changed = 0;
        for (RemoteDevPackageChange change : diff.changed()) {
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
            changed++;
        }
        int deleted = 0;
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
            deleted++;
        }
        return RemoteDevPackageClientResult.sent(changed, deleted);
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
                    return;
                }
                if (response.statusCode() / 100 != 2) {
                    LOG.debugf("Remote dev change poll against %s failed with status %d", config.redactedRemoteUrl(),
                            response.statusCode());
                    sleepAfterPollFailure();
                }
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
}
