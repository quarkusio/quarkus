package io.quarkus.vertx.http.runtime.devmode;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;

public class RemoteSyncHandler implements Handler<HttpServerRequest> {

    public static final String QUARKUS_PASSWORD = "X-Quarkus-Password";
    private static final Logger log = Logger.getLogger(RemoteSyncHandler.class);

    public static final String APPLICATION_QUARKUS = "application/quarkus-live-reload";
    public static final String QUARKUS_SESSION = "X-Quarkus-Session";
    public static final String QUARKUS_ERROR = "X-Quarkus-Error";
    public static final String QUARKUS_SESSION_COUNT = "X-Quarkus-Count";
    public static final String CONNECT = "/connect";
    public static final String DEV = "/dev";
    public static final String PROBE = "/probe"; //used to check that the server is back up after restart

    private static final long SESSION_TIMEOUT_MILLIS = 60000;
    private static final ReentrantLock SESSION_OPERATION_LOCK = new ReentrantLock(true);

    final String password;
    final Handler<HttpServerRequest> next;
    final HotReplacementContext hotReplacementContext;
    final String rootPath;
    private final RemoteDevBodyLimits bodyLimits;
    private final RemoteDevBodyAdmission bodyAdmission;
    private final RemoteDevBodySpoolStore bodySpoolStore;
    private final Set<RemoteDevBodyCollector> bodyCollectors = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    //all these are static to allow the handler to be recreated on hot reload
    //which makes lifecycle management a lot easier
    static volatile SessionState sessionState = SessionState.inactive();
    static volatile Throwable remoteProblem;
    static volatile boolean checkForChanges;

    public RemoteSyncHandler(String password, Handler<HttpServerRequest> next, HotReplacementContext hotReplacementContext,
            String rootPath) {
        this(password, next, hotReplacementContext, rootPath, Optional.empty());
    }

    public RemoteSyncHandler(String password, Handler<HttpServerRequest> next, HotReplacementContext hotReplacementContext,
            String rootPath, Optional<MemorySize> maxBodySize) {
        this(password, next, hotReplacementContext, rootPath, RemoteDevBodyLimits.from(maxBodySize));
    }

    RemoteSyncHandler(String password, Handler<HttpServerRequest> next, HotReplacementContext hotReplacementContext,
            String rootPath, RemoteDevBodyLimits bodyLimits) {
        this.password = password;
        this.next = next;
        this.hotReplacementContext = hotReplacementContext;
        this.rootPath = rootPath;
        this.bodyLimits = bodyLimits;
        this.bodyAdmission = new RemoteDevBodyAdmission(bodyLimits);
        this.bodySpoolStore = new RemoteDevBodySpoolStore(this);
    }

    public static void doPreScan() {
        SessionState state = sessionState;
        if (!state.isActive(System.currentTimeMillis())) {
            return;
        }
        synchronized (RemoteSyncHandler.class) {
            checkForChanges = true;
            //if there is a current dev request this will unblock it
            RemoteSyncHandler.class.notifyAll();
            try {
                RemoteSyncHandler.class.wait(30000);
            } catch (InterruptedException e) {
                log.debug("interrupted", e);
            }
        }
    }

    @Override
    public void handle(HttpServerRequest event) {
        final String type = event.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (APPLICATION_QUARKUS.equals(type)) {
            handleRequest(event);
            return;
        }
        next.handle(event);
    }

    private void handleRequest(HttpServerRequest event) {
        if (event.method().equals(HttpMethod.PUT)) {
            handlePut(event);
        } else if (event.method().equals(HttpMethod.DELETE)) {
            executeBlocking(() -> {
                handleDelete(event);
                return null;
            });
        } else if (event.method().equals(HttpMethod.POST)) {
            if (event.path().endsWith(DEV)) {
                handleDev(event);
            } else if (event.path().endsWith(CONNECT)) {
                handleConnect(event);
            } else if (event.path().endsWith(PROBE)) {
                event.response().end();
            } else {
                event.response().putHeader(QUARKUS_ERROR, "Unknown path " + event.path()
                        + " make sure your remote dev URL is pointing to the context root for your Quarkus instance, and not to a sub path.")
                        .setStatusCode(404).end();
            }
        } else {
            event.response()
                    .putHeader(QUARKUS_ERROR, "Unknown method " + event.method() + " this is not a valid remote dev request")
                    .setStatusCode(405).end();
        }
    }

    private void handleDev(HttpServerRequest event) {
        SessionRequest sessionRequest = readSessionRequest(event);
        if (sessionRequest == null) {
            return;
        }
        collectBody(event, body -> executeBlocking(() -> {
            processDev(event, sessionRequest, body);
            return null;
        }).onFailure(failure -> {
            body.close();
            bodyProcessingFailed(event, failure);
        }));
    }

    private void handleConnect(HttpServerRequest event) {
        collectBody(event, body -> executeBlocking(() -> {
            processConnect(event, body);
            return null;
        }).onFailure(failure -> {
            body.close();
            bodyProcessingFailed(event, failure);
        }));
    }

    private void processConnect(HttpServerRequest event, RemoteDevBodyCollector.CompletedBody body) {
        try {
            String rp = event.headers().get(QUARKUS_PASSWORD);
            String bodyHash = sha256(body);
            String compare = HashUtil.sha256(bodyHash + password);
            if (!constantTimeEquals(compare, rp)) {
                log.error("Incorrect password");
                event.response().putHeader(QUARKUS_ERROR, "Incorrect password").setStatusCode(401).end();
                return;
            }
            RemoteDevState state = readRemoteDevState(body);
            body.close();
            SESSION_OPERATION_LOCK.lock();
            try {
                Set<String> files = hotReplacementContext.syncState(state.getFileHashes());
                if (state.getAugmentProblem() != null) {
                    hotReplacementContext.setRemoteProblem(state.getAugmentProblem());
                }
                SecureRandom r = new SecureRandom();
                byte[] sessionId = new byte[40];
                r.nextBytes(sessionId);
                String session = Base64.getEncoder().encodeToString(sessionId);
                sessionState = new SessionState(session, 0,
                        System.currentTimeMillis() + SESSION_TIMEOUT_MILLIS);
                remoteProblem = state.getAugmentProblem();
                event.response().headers().set(QUARKUS_SESSION, session);
                event.response().end(String.join(";", files));
            } finally {
                SESSION_OPERATION_LOCK.unlock();
            }
        } catch (MalformedRemoteDevBodyException e) {
            event.response().putHeader(QUARKUS_ERROR, "Malformed remote-dev request body").setStatusCode(400).end();
        } catch (Exception e) {
            log.error("Connect failed", e);
            event.response().putHeader(QUARKUS_ERROR, "Remote-dev request processing failed").setStatusCode(500).end();
        } finally {
            body.close();
        }
    }

    private void handlePut(HttpServerRequest event) {
        SessionRequest sessionRequest = readSessionRequest(event);
        if (sessionRequest == null) {
            return;
        }
        collectBody(event, body -> executeBlocking(() -> {
            processPut(event, sessionRequest, body);
            return null;
        }).onFailure(failure -> {
            body.close();
            bodyProcessingFailed(event, failure);
        }));
    }

    private void processPut(HttpServerRequest event, SessionRequest request,
            RemoteDevBodyCollector.CompletedBody body) {
        try {
            String bodyHash = sha256(body);
            if (!authenticateSession(event, request, bodyHash)) {
                return;
            }
            SessionState expected = validateSession(event, request);
            if (expected == null) {
                return;
            }
            byte[] bytes = body.readAllBytes();
            body.close();
            if (!commitSessionOperation(event, request, expected,
                    () -> hotReplacementContext.updateFile(stripRootPath(event.path()), bytes))) {
                return;
            }
        } catch (IllegalArgumentException e) {
            log.warn("Rejected remote-dev file update", e);
            event.response().setStatusCode(400).end();
            return;
        } catch (Exception e) {
            log.error("Failed to update file", e);
            event.response().setStatusCode(500).end();
            return;
        } finally {
            body.close();
        }
        event.response().end();
    }

    private String stripRootPath(String path) {
        return path.startsWith(rootPath)
                ? path.substring(rootPath.length())
                : path;
    }

    private void handleDelete(HttpServerRequest event) {
        try {
            SessionRequest request = readSessionRequest(event);
            if (request == null) {
                return;
            }
            String dataHash = HashUtil.sha256(event.path().getBytes(StandardCharsets.UTF_8));
            if (!authenticateSession(event, request, dataHash)) {
                return;
            }
            SessionState expected = validateSession(event, request);
            if (expected == null || !commitSessionOperation(event, request, expected,
                    () -> hotReplacementContext.deleteFile(stripRootPath(event.path())))) {
                return;
            }
            event.response().end();
        } catch (IllegalArgumentException e) {
            log.warn("Rejected remote-dev file deletion", e);
            event.response().setStatusCode(400).end();
        } catch (Exception e) {
            log.error("Failed to delete file", e);
            event.response().setStatusCode(500).end();
        }
    }

    private SessionRequest readSessionRequest(HttpServerRequest event) {
        String ses = event.headers().get(QUARKUS_SESSION);
        String sessionCount = event.headers().get(QUARKUS_SESSION_COUNT);
        if (ses == null || sessionCount == null) {
            log.error("No session count provided");
            //not really sure what status code makes sense here
            //Non-Authoritative Information seems as good as any
            event.response().setStatusCode(203).end();
            return null;
        }
        final int sc;
        try {
            sc = Integer.parseInt(sessionCount);
        } catch (NumberFormatException e) {
            log.error("Invalid session count");
            event.response().setStatusCode(203).end();
            return null;
        }
        if (sc <= 0) {
            log.error("Invalid session count");
            event.response().setStatusCode(203).end();
            return null;
        }
        return new SessionRequest(ses, sc, event.headers().get(QUARKUS_PASSWORD));
    }

    private boolean authenticateSession(HttpServerRequest event, SessionRequest request, String dataHash) {
        String compare = HashUtil.sha256(dataHash + request.sessionId() + request.counter() + password);
        if (!constantTimeEquals(compare, request.authenticator())) {
            log.error("Incorrect password");
            event.response().setStatusCode(401).end();
            return false;
        }
        return true;
    }

    private SessionState validateSession(HttpServerRequest event, SessionRequest request) {
        SESSION_OPERATION_LOCK.lock();
        try {
            long now = System.currentTimeMillis();
            SessionState current = sessionState;
            if (!current.isActive(now)) {
                sessionState = SessionState.inactive();
                log.error("Invalid session");
                event.response().setStatusCode(203).end();
                return null;
            }
            if (!current.id().equals(request.sessionId()) || request.counter() <= current.acceptedCounter()) {
                log.error("Invalid session");
                //not really sure what status code makes sense here
                //Non-Authoritative Information seems as good as any
                event.response().setStatusCode(203).end();
                return null;
            }
            return current;
        } finally {
            SESSION_OPERATION_LOCK.unlock();
        }
    }

    private boolean acceptSession(HttpServerRequest event, SessionRequest request) {
        SESSION_OPERATION_LOCK.lock();
        try {
            long now = System.currentTimeMillis();
            SessionState current = sessionState;
            if (!current.isActive(now) || !current.id().equals(request.sessionId())
                    || request.counter() <= current.acceptedCounter()) {
                if (!current.isActive(now)) {
                    sessionState = SessionState.inactive();
                }
                log.error("Invalid session");
                event.response().setStatusCode(203).end();
                return false;
            }
            sessionState = new SessionState(current.id(), request.counter(), now + SESSION_TIMEOUT_MILLIS);
            return true;
        } finally {
            SESSION_OPERATION_LOCK.unlock();
        }
    }

    private boolean commitSessionOperation(HttpServerRequest event, SessionRequest request, SessionState expected,
            CheckedOperation operation) throws Exception {
        SESSION_OPERATION_LOCK.lock();
        try {
            long now = System.currentTimeMillis();
            SessionState current = sessionState;
            if (!current.equals(expected) || !current.isActive(now) || !current.id().equals(request.sessionId())
                    || request.counter() <= current.acceptedCounter()) {
                if (!current.isActive(now)) {
                    sessionState = SessionState.inactive();
                }
                log.error("Invalid session");
                event.response().setStatusCode(203).end();
                return false;
            }
            sessionState = new SessionState(current.id(), request.counter(), now + SESSION_TIMEOUT_MILLIS);
            operation.run();
            return true;
        } finally {
            SESSION_OPERATION_LOCK.unlock();
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        byte[] actualBytes = new byte[expectedBytes.length];
        boolean valid = actual != null && actual.length() == expectedBytes.length;
        if (valid) {
            for (int i = 0; i < actual.length(); i++) {
                char value = actual.charAt(i);
                if (Character.digit(value, 16) == -1) {
                    valid = false;
                }
                actualBytes[i] = (byte) value;
            }
        }
        return MessageDigest.isEqual(expectedBytes, actualBytes) & valid;
    }

    <T> Future<T> executeBlocking(Callable<T> action) {
        return VertxCoreRecorder.getVertx().get().executeBlocking(action, false);
    }

    @FunctionalInterface
    private interface CheckedOperation {
        void run() throws Exception;
    }

    record SessionState(String id, int acceptedCounter, long expiresAt) {

        static SessionState inactive() {
            return new SessionState(null, 0, 0);
        }

        boolean isActive(long now) {
            return id != null && now <= expiresAt;
        }
    }

    private record SessionRequest(String sessionId, int counter, String authenticator) {
    }

    /**
     * Creates an {@link ObjectInputStream} with a deny-by-default deserialization filter
     * for the remote dev protocol.
     * <p>
     * The remote dev client ({@code HttpRemoteDevClient}) serializes {@link RemoteDevState}
     * (containing a {@code HashMap<String, String>} of file hashes and a {@code Throwable}
     * for build errors) and standalone {@code Throwable} instances. The allowlist covers
     * the class families needed to deserialize these objects:
     * <ul>
     * <li>{@code io.quarkus.dev.spi.RemoteDevState}: the protocol's state object</li>
     * <li>{@code java.lang.*}: String, Throwable/Exception subclasses, StackTraceElement, Number</li>
     * <li>{@code java.io.*}: IOException and subclasses thrown during builds</li>
     * <li>{@code java.util.*}: HashMap, ArrayList (suppressed exceptions list)</li>
     * <li>{@code java.math.*}: referenced by serialization internals</li>
     * </ul>
     * The trailing {@code !*} rejects everything else.
     */
    private static ObjectInputStream createFilteredObjectInputStream(InputStream data, long maxBytes) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(data));
        ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                "maxdepth=100;"
                        + "maxrefs=1000000;"
                        + "maxarray=1000000;"
                        + "maxbytes=" + maxBytes + ";"
                        + "io.quarkus.dev.spi.RemoteDevState;"
                        + "java.lang.*;"
                        + "java.io.*;"
                        + "java.util.*;"
                        + "java.math.*;"
                        + "!*"));
        return ois;
    }

    private RemoteDevState readRemoteDevState(RemoteDevBodyCollector.CompletedBody body)
            throws IOException, MalformedRemoteDevBodyException {
        Object value = readSerialized(body);
        if (!(value instanceof RemoteDevState state)) {
            throw new MalformedRemoteDevBodyException("Expected remote-dev state");
        }
        return state;
    }

    private Throwable readRemoteProblem(RemoteDevBodyCollector.CompletedBody body)
            throws IOException, MalformedRemoteDevBodyException {
        Object value = readSerialized(body);
        if (value != null && !(value instanceof Throwable)) {
            throw new MalformedRemoteDevBodyException("Expected remote-dev problem");
        }
        return (Throwable) value;
    }

    private Object readSerialized(RemoteDevBodyCollector.CompletedBody body)
            throws IOException, MalformedRemoteDevBodyException {
        try (InputStream stream = body.openInputStream();
                ObjectInputStream input = createFilteredObjectInputStream(stream, bodyLimits.requestLimit())) {
            return input.readObject();
        } catch (EOFException e) {
            throw new MalformedRemoteDevBodyException("Rejected serialized remote-dev request", e);
        } catch (ObjectStreamException e) {
            throw new MalformedRemoteDevBodyException("Malformed serialized remote-dev request", e);
        } catch (ClassNotFoundException e) {
            throw new MalformedRemoteDevBodyException("Unsupported serialized remote-dev request", e);
        }
    }

    private static String sha256(RemoteDevBodyCollector.CompletedBody body) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        byte[] buffer = new byte[8192];
        try (InputStream input = body.openInputStream()) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void processDev(HttpServerRequest event, SessionRequest request,
            RemoteDevBodyCollector.CompletedBody body) {
        try {
            String bodyHash = sha256(body);
            if (!authenticateSession(event, request, bodyHash) || !acceptSession(event, request)) {
                return;
            }
            Throwable problem = readRemoteProblem(body);
            body.close();
            //update the problem if it has changed
            if (problem != null || remoteProblem != null) {
                remoteProblem = problem;
                hotReplacementContext.setRemoteProblem(problem);
            }
            synchronized (RemoteSyncHandler.class) {
                RemoteSyncHandler.class.notifyAll();
                try {
                    RemoteSyncHandler.class.wait(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.debug("interrupted", e);
                }
                if (checkForChanges) {
                    checkForChanges = false;
                    event.response().setStatusCode(200);
                } else {
                    event.response().setStatusCode(204);
                }
                event.response().end();
            }
        } catch (MalformedRemoteDevBodyException e) {
            event.response().putHeader(QUARKUS_ERROR, "Malformed remote-dev request body").setStatusCode(400).end();
        } catch (RejectedExecutionException e) {
            //everything is shut down
            //likely in the middle of a restart
            event.connection().close();
        } catch (Exception e) {
            log.error("Remote-dev request failed", e);
            event.response().putHeader(QUARKUS_ERROR, "Remote-dev request processing failed").setStatusCode(500).end();
        } finally {
            body.close();
        }
    }

    private void collectBody(HttpServerRequest event,
            java.util.function.Consumer<RemoteDevBodyCollector.CompletedBody> action) {
        if (closed.get()) {
            rejectBody(event, 503, "Remote dev is restarting");
            return;
        }
        RemoteDevBodyCollector.start(this, event, bodyLimits, bodyAdmission, bodySpoolStore, action);
    }

    void collectorStarted(RemoteDevBodyCollector collector) {
        bodyCollectors.add(collector);
        if (closed.get()) {
            collector.cancel();
        }
    }

    void collectorClosed(RemoteDevBodyCollector collector) {
        bodyCollectors.remove(collector);
    }

    void rejectBody(HttpServerRequest request, int status, String message) {
        request.handler(ignored -> {
        }).exceptionHandler(ignored -> {
        }).endHandler(ignored -> {
        }).resume();
        var response = request.response().putHeader(QUARKUS_ERROR, message).setStatusCode(status);
        if (status == 503) {
            response.putHeader("Retry-After", "1");
        }
        boolean multiplexed = request.version() == HttpVersion.HTTP_2 || request.version() == HttpVersion.HTTP_3;
        if (!multiplexed) {
            response.putHeader(HttpHeaderNames.CONNECTION, "close");
        }
        Future<Void> ended = response.end();
        if (!multiplexed) {
            if (ended == null) {
                request.connection().close();
            } else {
                ended.onComplete(ignored -> request.connection().close());
            }
        }
    }

    void bodyCollectionFailed(HttpServerRequest request, Throwable failure) {
        log.error("Failed to collect remote-dev request body", failure);
        rejectBody(request, 500, "Remote-dev request body processing failed");
    }

    void bodyProcessingFailed(HttpServerRequest request, Throwable failure) {
        log.error("Failed to process remote-dev request body", failure);
        request.response().putHeader(QUARKUS_ERROR, "Remote-dev request processing failed").setStatusCode(500).end();
    }

    void bodyCleanupFailed() {
        // Do not attach the exception because file-system exceptions can expose the private spool path.
        log.error("Failed to clean up a remote-dev request body");
    }

    Path bodySpoolDirectory() {
        return bodySpoolStore.directory();
    }

    private static final class MalformedRemoteDevBodyException extends Exception {

        private MalformedRemoteDevBodyException(String message) {
            super(message);
        }

        private MalformedRemoteDevBodyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            bodySpoolStore.close();
            for (RemoteDevBodyCollector collector : Set.copyOf(bodyCollectors)) {
                collector.cancel();
            }
        }
        synchronized (RemoteSyncHandler.class) {
            RemoteSyncHandler.class.notifyAll();
        }
    }
}
