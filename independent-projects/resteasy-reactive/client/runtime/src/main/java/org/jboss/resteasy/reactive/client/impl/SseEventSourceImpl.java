package org.jboss.resteasy.reactive.client.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.net.impl.ConnectionBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import org.jboss.resteasy.reactive.common.util.CommonSseUtil;

public class SseEventSourceImpl implements SseEventSource, Handler<Long> {

    private TimeUnit reconnectUnit;
    private long reconnectDelay;

    private WebTargetImpl webTarget;
    // this tracks user request to open/close
    private volatile boolean isOpen;
    // this tracks whether we have a connection open
    private volatile boolean isInProgress;

    private List<Consumer<InboundSseEvent>> consumers = new ArrayList<>();
    private List<Consumer<Throwable>> errorListeners = new ArrayList<>();
    private List<Runnable> completionListeners = new ArrayList<>();
    private HttpConnection connection;
    private SseParser sseParser;
    private long timerId = -1;
    private boolean receivedClientClose;

    public SseEventSourceImpl(WebTargetImpl webTarget, long reconnectDelay, TimeUnit reconnectUnit) {
        // tests set a null endpoint
        Objects.requireNonNull(reconnectUnit);
        if (reconnectDelay <= 0)
            throw new IllegalArgumentException("Delay must be > 0: " + reconnectDelay);
        this.webTarget = webTarget;
        this.reconnectDelay = reconnectDelay;
        this.reconnectUnit = reconnectUnit;
        this.sseParser = new SseParser(this);
    }

    WebTargetImpl getWebTarget() {
        return webTarget;
    }

    @Override
    public synchronized void register(Consumer<InboundSseEvent> onEvent) {
        consumers.add(onEvent);
    }

    @Override
    public synchronized void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError) {
        consumers.add(onEvent);
        errorListeners.add(onError);
    }

    @Override
    public synchronized void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError, Runnable onComplete) {
        consumers.add(onEvent);
        errorListeners.add(onError);
        completionListeners.add(onComplete);
    }

    @Override
    public synchronized void open() {
        if (isOpen)
            return;
        isOpen = true;
        connect();
    }

    // CALL WITH THE LOCK
    private void connect() {
        if (isInProgress)
            return;
        isInProgress = true;
        // ignore previous client closes
        receivedClientClose = false;
        AsyncInvokerImpl invoker = (AsyncInvokerImpl) webTarget.request().rx();
        RestClientRequestContext restClientRequestContext = invoker.performRequestInternal("GET", null, null, false);
        restClientRequestContext.getResult().handle((response, throwable) -> {
            if (throwable != null)
                receiveThrowable(throwable);
            else {
                // FIXME: check response
                registerOnClient(restClientRequestContext.getVertxClientResponse());
            }
            return null;
        });
    }

    /**
     * Allows the HTTP client to register for SSE after it has made the request
     */
    synchronized void registerAfterRequest(HttpClientResponse vertxClientResponse) {
        if (isOpen)
            throw new IllegalStateException("Was already open");
        isOpen = true;
        registerOnClient(vertxClientResponse);
    }

    private void registerOnClient(HttpClientResponse vertxClientResponse) {
        // make sure we get exceptions on the response, like close events, otherwise they
        // will be logged as errors by vertx
        vertxClientResponse.exceptionHandler(t -> {
            if (t == ConnectionBase.CLOSED_EXCEPTION) {
                // we can ignore this one since we registered a closeHandler
            } else {
                receiveThrowable(t);
            }
        });
        // since we registered our exception handler, let's remove the request exception handler
        // that is set in ClientSendRequestHandler
        vertxClientResponse.request().exceptionHandler(null);
        connection = vertxClientResponse.request().connection();
        connection.closeHandler(v -> {
            close(true);
        });
        String sseContentTypeHeader = vertxClientResponse.getHeader(CommonSseUtil.SSE_CONTENT_TYPE);
        sseParser.setSseContentTypeHeader(sseContentTypeHeader);
        vertxClientResponse.handler(sseParser);
        vertxClientResponse.endHandler(v -> {
            close(true);
        });
        vertxClientResponse.resume();
    }

    private void receiveThrowable(Throwable throwable) {
        for (Consumer<Throwable> errorListener : errorListeners) {
            errorListener.accept(throwable);
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean close(long timeout, TimeUnit unit) {
        close(false);
        return true;
    }

    private synchronized void close(boolean clientClosed) {
        if (!isOpen) {
            return;
        }
        if (clientClosed) {
            // do not react more than once on client closing
            if (this.receivedClientClose) {
                return;
            }
        }
        // it's possible that the client closed our connection, then we registered a reconnect timer
        // and then the user is closing us, so we don't have a connection yet
        if (connection != null) {
            connection.close();
        }
        connection = null;
        isInProgress = false;
        boolean notifyCompletion = true;
        if (!clientClosed) {
            isOpen = false;
            if (receivedClientClose) {
                // do not notify completion if we already did as part of the client closing
                notifyCompletion = false;
            }
        } else {
            receivedClientClose = true;
        }
        if (notifyCompletion) {
            // notify completion before reconnecting
            for (Runnable runnable : completionListeners) {
                runnable.run();
            }
        }
        Vertx vertx = webTarget.getRestClient().getVertx();
        // did we already try to reconnect?
        if (timerId != -1) {
            // cancel any previous timer
            vertx.cancelTimer(timerId);
            timerId = -1;
        }
        // schedule a new reconnect if the client closed us
        if (clientClosed) {
            timerId = vertx.setTimer(TimeUnit.MILLISECONDS.convert(reconnectDelay, reconnectUnit), this);
        }
    }

    public synchronized void fireEvent(InboundSseEventImpl event) {
        // spec says to do this
        if (event.isReconnectDelaySet()) {
            reconnectDelay = event.getReconnectDelay();
            reconnectUnit = TimeUnit.MILLISECONDS;
        }
        for (Consumer<InboundSseEvent> consumer : consumers) {
            consumer.accept(event);
        }
    }

    @Override
    public synchronized void handle(Long event) {
        // ignore a timeout if it's not the last one we set
        if (timerId != event.longValue()) {
            return;
        }
        // also ignore a reconnect order if the user closed this
        if (!isOpen) {
            return;
        }
        connect();
    }

    // For tests
    public SseParser getSseParser() {
        return sseParser;
    }
}
