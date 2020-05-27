package io.quarkus.vertx.http.runtime.devmode;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.runtime.ExecutorRecorder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

public class RemoteSyncHandler implements Handler<HttpServerRequest> {

    public static final String QUARKUS_PASSWORD = "X-Quarkus-Password";
    private static final Logger log = Logger.getLogger(RemoteSyncHandler.class);

    public static final String APPLICATION_QUARKUS = "application/quarkus-live-reload";
    public static final String QUARKUS_SESSION = "X-Quarkus-Session";
    public static final String CONNECT = "/connect";
    public static final String DEV = "/dev";

    final String password;
    final Handler<HttpServerRequest> next;
    final HotReplacementContext hotReplacementContext;

    //all these are static to allow the handler to be recreated on hot reload
    //which makes lifecycle management a lot easier
    static volatile String currentSession;
    static volatile long currentSessionTimeout;
    static volatile Throwable remoteProblem;
    static boolean checkForChanges;

    public RemoteSyncHandler(String password, Handler<HttpServerRequest> next, HotReplacementContext hotReplacementContext) {
        this.password = password;
        this.next = next;
        this.hotReplacementContext = hotReplacementContext;
        hotReplacementContext.addPreScanStep(new Runnable() {
            @Override
            public void run() {
                if (currentSession == null) {
                    return;
                }
                doPreScan();
            }
        });
    }

    private void doPreScan() {

        synchronized (RemoteSyncHandler.class) {
            checkForChanges = true;
            //if there is a current dev request this will unblock it
            RemoteSyncHandler.class.notifyAll();
            try {
                RemoteSyncHandler.class.wait(30000);
            } catch (InterruptedException e) {
                log.error("interrupted", e);
            }
        }
    }

    @Override
    public void handle(HttpServerRequest event) {
        long time = System.currentTimeMillis();
        if (time > currentSessionTimeout) {
            currentSession = null;
        }
        final String type = event.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (APPLICATION_QUARKUS.equals(type)) {
            String rp = event.headers().get(QUARKUS_PASSWORD);
            if (!password.equals(rp)) {
                log.error("Incorrect password");
                event.response().setStatusCode(401).end();
                return;
            }
            currentSessionTimeout = time + 60000;
            ExecutorRecorder.getCurrent().execute(new Runnable() {
                @Override
                public void run() {
                    handleRequest(event);
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
            if (event.path().equals(DEV)) {
                handleDev(event);
            } else if (event.path().equals(CONNECT)) {
                handleConnect(event);
            } else {
                event.response().setStatusCode(404).end();
            }
        } else {
            event.response().setStatusCode(404).end();
        }

    }

    private void handleDev(HttpServerRequest event) {
        if (checkSession(event))
            return;
        event.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer b) {
                ExecutorRecorder.getCurrent().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Throwable problem = (Throwable) new ObjectInputStream(new ByteArrayInputStream(b.getBytes()))
                                    .readObject();
                            //update the problem if it has changed
                            if (problem != null || remoteProblem != null) {
                                remoteProblem = problem;
                                hotReplacementContext.setRemoteProblem(problem);
                            }
                            synchronized (RemoteSyncHandler.class) {

                                RemoteSyncHandler.class.notifyAll();
                                RemoteSyncHandler.class.wait(10000);
                                if (checkForChanges) {
                                    checkForChanges = false;
                                    event.response().setStatusCode(200);
                                } else {
                                    event.response().setStatusCode(204);
                                }
                                event.response().end();
                            }
                        } catch (Exception e) {
                            log.error("Connect failed", e);
                            event.response().setStatusCode(500).end();
                        }
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
                try {
                    SecureRandom r = new SecureRandom();
                    byte[] sessionId = new byte[20];
                    r.nextBytes(sessionId);
                    currentSession = Base64.getEncoder().encodeToString(sessionId);
                    RemoteDevState state = (RemoteDevState) new ObjectInputStream(new ByteArrayInputStream(b.getBytes()))
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
        if (checkSession(event))
            return;
        event.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                hotReplacementContext.updateFile(event.path(), buffer.getBytes());
                event.response().end();
            }
        }).exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable error) {
                log.error("Failed writing hot replacement data", error);
                event.response().setStatusCode(500);
                event.response().end();
            }
        }).resume();
    }

    private void handleDelete(HttpServerRequest event) {
        if (checkSession(event))
            return;
        hotReplacementContext.updateFile(event.path(), null);
        event.response().end();
    }

    private boolean checkSession(HttpServerRequest event) {
        String ses = event.headers().get(QUARKUS_SESSION);
        if (!Objects.equals(ses, currentSession)) {
            log.error("Invalid session");
            //not really sure what status code makes sense here
            //Non-Authoritative Information seems as good as any
            event.response().setStatusCode(203).end();
            return true;
        }
        return false;
    }
}
