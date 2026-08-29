package io.quarkus.vertx.http.runtime.devmode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

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

    //all these are static to allow the handler to be recreated on hot reload
    //which makes lifecycle management a lot easier
    static volatile SessionState sessionState = SessionState.inactive();
    static volatile Throwable remoteProblem;
    static volatile boolean checkForChanges;

    public RemoteSyncHandler(String password, Handler<HttpServerRequest> next, HotReplacementContext hotReplacementContext,
            String rootPath) {
        this.password = password;
        this.next = next;
        this.hotReplacementContext = hotReplacementContext;
        this.rootPath = rootPath;
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
            executeBlocking(new Callable<Void>() {
                @Override
                public Void call() {
                    handleRequest(event);
                    return null;
                }
            });
            return;
        }
        next.handle(event);
    }

    private void handleRequest(HttpServerRequest event) {
        if (event.method().equals(HttpMethod.PUT)) {
            handlePut(event);
        } else if (event.method().equals(HttpMethod.DELETE)) {
            handleDelete(event);
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
        event.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer b) {
                executeBlocking(new Callable<Void>() {
                    @Override
                    public Void call() {
                        try {
                            withAuthenticatedSession(event, b.getBytes(), () -> {
                                Throwable problem;
                                try (ObjectInputStream input = createFilteredObjectInputStream(b.getBytes())) {
                                    problem = (Throwable) input.readObject();
                                }
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
                            });
                        } catch (RejectedExecutionException e) {
                            //everything is shut down
                            //likely in the middle of a restart
                            event.connection().close();
                        } catch (Exception e) {
                            log.error("Connect failed", e);
                            event.response().setStatusCode(500).end();
                        }
                        return null;
                    }
                });
            }
        }).exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable t) {
                log.error("dev request failed", t);
                event.response().setStatusCode(500).end();
            }
        }).resume();
    }

    private void handleConnect(HttpServerRequest event) {
        event.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer b) {
                executeBlocking(() -> {
                    processConnect(event, b);
                    return null;
                });
            }
        }).exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable t) {
                log.error("Connect failed", t);
                event.response().setStatusCode(500).end();
            }
        }).resume();
    }

    private void processConnect(HttpServerRequest event, Buffer body) {
        try {
            String rp = event.headers().get(QUARKUS_PASSWORD);
            String bodyHash = HashUtil.sha256(body.getBytes());
            String compare = HashUtil.sha256(bodyHash + password);
            if (!constantTimeEquals(compare, rp)) {
                log.error("Incorrect password");
                event.response().putHeader(QUARKUS_ERROR, "Incorrect password").setStatusCode(401).end();
                return;
            }
            SESSION_OPERATION_LOCK.lock();
            try {
                RemoteDevState state;
                try (ObjectInputStream input = createFilteredObjectInputStream(body.getBytes())) {
                    state = (RemoteDevState) input.readObject();
                }
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
        } catch (Exception e) {
            log.error("Connect failed", e);
            event.response().setStatusCode(500).end();
        }
    }

    private void handlePut(HttpServerRequest event) {
        event.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                executeBlocking(() -> {
                    processPut(event, buffer);
                    return null;
                });
            }
        }).exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable error) {
                log.error("Failed writing live reload data", error);
                event.response().setStatusCode(500);
                event.response().end();
            }
        }).resume();
    }

    private void processPut(HttpServerRequest event, Buffer buffer) {
        try {
            if (withAuthenticatedSession(event, buffer.getBytes(),
                    () -> hotReplacementContext.updateFile(stripRootPath(event.path()), buffer.getBytes()))) {
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
            if (withAuthenticatedSession(event, event.path().getBytes(StandardCharsets.UTF_8),
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

    private boolean withAuthenticatedSession(HttpServerRequest event, byte[] data, CheckedOperation operation)
            throws Exception {
        String ses = event.headers().get(QUARKUS_SESSION);
        String sessionCount = event.headers().get(QUARKUS_SESSION_COUNT);
        if (sessionCount == null) {
            log.error("No session count provided");
            //not really sure what status code makes sense here
            //Non-Authoritative Information seems as good as any
            event.response().setStatusCode(203).end();
            return true;
        }
        final int sc;
        try {
            sc = Integer.parseInt(sessionCount);
        } catch (NumberFormatException e) {
            log.error("Invalid session count");
            event.response().setStatusCode(203).end();
            return true;
        }
        if (sc <= 0) {
            log.error("Invalid session count");
            event.response().setStatusCode(203).end();
            return true;
        }

        String dataHash = "";
        if (data != null) {
            dataHash = HashUtil.sha256(data);
        }
        String rp = event.headers().get(QUARKUS_PASSWORD);
        String compare = HashUtil.sha256(dataHash + ses + sc + password);
        if (!constantTimeEquals(compare, rp)) {
            log.error("Incorrect password");
            event.response().setStatusCode(401).end();
            return true;
        }
        SESSION_OPERATION_LOCK.lock();
        try {
            long now = System.currentTimeMillis();
            SessionState current = sessionState;
            if (!current.isActive(now)) {
                sessionState = SessionState.inactive();
                log.error("Invalid session");
                event.response().setStatusCode(203).end();
                return true;
            }
            if (!current.id().equals(ses) || sc <= current.acceptedCounter()) {
                log.error("Invalid session");
                //not really sure what status code makes sense here
                //Non-Authoritative Information seems as good as any
                event.response().setStatusCode(203).end();
                return true;
            }
            sessionState = new SessionState(current.id(), sc, now + SESSION_TIMEOUT_MILLIS);
            operation.run();
            return false;
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

    void executeBlocking(Callable<Void> action) {
        VertxCoreRecorder.getVertx().get().executeBlocking(action, false);
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
    private static ObjectInputStream createFilteredObjectInputStream(byte[] data) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                "io.quarkus.dev.spi.RemoteDevState;"
                        + "java.lang.*;"
                        + "java.io.*;"
                        + "java.util.*;"
                        + "java.math.*;"
                        + "!*"));
        return ois;
    }

    public void close() {
        synchronized (RemoteSyncHandler.class) {
            RemoteSyncHandler.class.notifyAll();
        }
    }
}
