package io.quarkus.vertx.http.runtime.devmode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

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

    final String password;
    final Handler<HttpServerRequest> next;
    final HotReplacementContext hotReplacementContext;
    final String rootPath;

    //all these are static to allow the handler to be recreated on hot reload
    //which makes lifecycle management a lot easier
    static volatile String currentSession;
    //incrementing counter to prevent replay attacks
    static volatile int currentSessionCounter;
    static volatile long currentSessionTimeout;
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
        if (currentSession == null) {
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
        long time = System.currentTimeMillis();
        if (time > currentSessionTimeout) {
            currentSession = null;
            currentSessionCounter = 0;
        }
        final String type = event.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (APPLICATION_QUARKUS.equals(type)) {
            currentSessionTimeout = time + 60000;
            VertxCoreRecorder.getVertx().get().executeBlocking(new Callable<Void>() {
                @Override
                public Void call() {
                    handleRequest(event);
                    return null;
                }
            }, false);
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
                if (checkSession(event, b.getBytes())) {
                    return;
                }
                VertxCoreRecorder.getVertx().get().executeBlocking(new Callable<Void>() {
                    @Override
                    public Void call() {
                        try {
                            Throwable problem = (Throwable) createFilteredObjectInputStream(b.getBytes())
                                    .readObject();
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
                }, false);
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
                try {

                    String rp = event.headers().get(QUARKUS_PASSWORD);
                    String bodyHash = HashUtil.sha256(b.getBytes());
                    String compare = HashUtil.sha256(bodyHash + password);
                    if (!compare.equals(rp)) {
                        log.error("Incorrect password");
                        event.response().putHeader(QUARKUS_ERROR, "Incorrect password").setStatusCode(401).end();
                        return;
                    }
                    SecureRandom r = new SecureRandom();
                    byte[] sessionId = new byte[40];
                    r.nextBytes(sessionId);
                    currentSession = Base64.getEncoder().encodeToString(sessionId);
                    currentSessionCounter = 0;
                    RemoteDevState state = (RemoteDevState) createFilteredObjectInputStream(b.getBytes())
                            .readObject();
                    remoteProblem = state.getAugmentProblem();
                    if (state.getAugmentProblem() != null) {
                        hotReplacementContext.setRemoteProblem(state.getAugmentProblem());
                    }
                    Set<String> files = hotReplacementContext.syncState(state.getFileHashes());
                    event.response().headers().set(QUARKUS_SESSION, currentSession);
                    event.response().end(String.join(";", files));

                } catch (Exception e) {
                    log.error("Connect failed", e);
                    event.response().setStatusCode(500).end();
                }

            }
        }).exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable t) {
                log.error("Connect failed", t);
                event.response().setStatusCode(500).end();
            }
        }).resume();
    }

    private void handlePut(HttpServerRequest event) {
        event.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                if (checkSession(event, buffer.getBytes())) {
                    return;
                }
                try {
                    String path = stripRootPath(event.path());
                    hotReplacementContext.updateFile(path, buffer.getBytes());
                } catch (Exception e) {
                    log.error("Failed to update file", e);
                }
                event.response().end();
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

    private String stripRootPath(String path) {
        return path.startsWith(rootPath)
                ? path.substring(rootPath.length())
                : path;
    }

    private void handleDelete(HttpServerRequest event) {
        if (checkSession(event, event.path().getBytes(StandardCharsets.UTF_8)))
            return;
        hotReplacementContext.deleteFile(stripRootPath(event.path()));
        event.response().end();
    }

    private boolean checkSession(HttpServerRequest event, byte[] data) {
        String ses = event.headers().get(QUARKUS_SESSION);
        String sessionCount = event.headers().get(QUARKUS_SESSION_COUNT);
        if (sessionCount == null) {
            log.error("No session count provided");
            //not really sure what status code makes sense here
            //Non-Authoritative Information seems as good as any
            event.response().setStatusCode(203).end();
            return true;
        }
        int sc = Integer.parseInt(sessionCount);
        if (!Objects.equals(ses, currentSession) ||
                sc <= currentSessionCounter) {
            log.error("Invalid session");
            //not really sure what status code makes sense here
            //Non-Authoritative Information seems as good as any
            event.response().setStatusCode(203).end();
            return true;
        }
        currentSessionCounter = sc;

        String dataHash = "";
        if (data != null) {
            dataHash = HashUtil.sha256(data);
        }
        String rp = event.headers().get(QUARKUS_PASSWORD);
        String compare = HashUtil.sha256(dataHash + ses + sc + password);
        if (!compare.equals(rp)) {
            log.error("Incorrect password");
            event.response().setStatusCode(401).end();
            return true;
        }
        return false;
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
