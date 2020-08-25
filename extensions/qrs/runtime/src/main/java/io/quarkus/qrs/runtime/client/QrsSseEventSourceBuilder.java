package io.quarkus.qrs.runtime.client;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import javax.ws.rs.sse.SseEventSource.Builder;

public class QrsSseEventSourceBuilder extends SseEventSource.Builder {

    private WebTarget endpoint;
    private TimeUnit reconnectUnit;
    private long reconnectDelay;

    @Override
    protected Builder target(WebTarget endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    @Override
    public Builder reconnectingEvery(long delay, TimeUnit unit) {
        this.reconnectDelay = delay;
        this.reconnectUnit = unit;
        return this;
    }

    @Override
    public SseEventSource build() {
        return new QrsSseEventSource(endpoint, reconnectDelay, reconnectUnit);
    }

}
