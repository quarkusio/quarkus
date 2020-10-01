package io.quarkus.rest.runtime.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.net.impl.ConnectionBase;

public class QuarkusRestSseEventSource implements SseEventSource, Handler<Long> {

    private TimeUnit reconnectUnit;
    private long reconnectDelay;

    private QuarkusRestWebTarget webTarget;
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

    public QuarkusRestSseEventSource(QuarkusRestWebTarget webTarget, long reconnectDelay, TimeUnit reconnectUnit) {
        // tests set a null endpoint
        Objects.requireNonNull(reconnectUnit);
        if (reconnectDelay <= 0)
            throw new IllegalArgumentException("Delay must be > 0: " + reconnectDelay);
        this.webTarget = webTarget;
        this.reconnectDelay = reconnectDelay;
        this.reconnectUnit = reconnectUnit;
        this.sseParser = new SseParser(this);
    }

    QuarkusRestWebTarget getWebTarget() {
        return webTarget;
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent) {
        consumers.add(onEvent);
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError) {
        consumers.add(onEvent);
        errorListeners.add(onError);
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError, Runnable onComplete) {
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
        QuarkusRestAsyncInvoker invoker = (QuarkusRestAsyncInvoker) webTarget.request().rx();
        InvocationState invocationState = invoker.performRequestInternal("GET", null, null, false);
        invocationState.getResult().handle((response, throwable) -> {
            if (throwable != null)
                receiveThrowable(throwable);
            else {
                // FIXME: check response
                registerOnClient(invocationState.getVertxClientResponse());
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
                // FIXME: handle real exceptions
                t.printStackTrace();
            }
        });
        connection = vertxClientResponse.request().connection();
        connection.closeHandler(v -> {
            close(true);
        });
        vertxClientResponse.handler(sseParser);
        // FIXME: handle end of response rather than wait for end of connection
    }

    private void receiveThrowable(Throwable throwable) {
        // TODO Auto-generated method stub

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
        // it's possible that the client closed our connection, then we registered a reconnect timer
        // and then the user is closing us, so we don't have a connection yet
        if (connection != null) {
            connection.close();
        }
        connection = null;
        isInProgress = false;
        if (!clientClosed) {
            isOpen = false;
        }
        // notify completion before reconnecting
        for (Runnable runnable : completionListeners) {
            runnable.run();
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

    public void fireEvent(QuarkusRestInboundSseEvent event) {
        // spec says to do this
        if (event.isReconnectDelaySet()) {
            // this needs to be atomic
            synchronized (this) {
                reconnectDelay = event.getReconnectDelay();
                reconnectUnit = TimeUnit.MILLISECONDS;
            }
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
