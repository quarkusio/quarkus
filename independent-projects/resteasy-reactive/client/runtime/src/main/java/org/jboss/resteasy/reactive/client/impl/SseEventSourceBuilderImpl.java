package org.jboss.resteasy.reactive.client.impl;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import javax.ws.rs.sse.SseEventSource.Builder;

public class SseEventSourceBuilderImpl extends SseEventSource.Builder {

    private WebTarget endpoint;
    // defaults set by spec
    private TimeUnit reconnectUnit = TimeUnit.MILLISECONDS;
    private long reconnectDelay = 500;

    @Override
    protected Builder target(WebTarget endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    @Override
    public Builder reconnectingEvery(long delay, TimeUnit unit) {
        Objects.requireNonNull(unit);
        if (delay <= 0)
            throw new IllegalArgumentException("Delay must be > 0: " + delay);
        this.reconnectDelay = delay;
        this.reconnectUnit = unit;
        return this;
    }

    @Override
    public SseEventSource build() {
        return new SseEventSourceImpl((WebTargetImpl) endpoint, endpoint.request(), reconnectDelay, reconnectUnit);
    }

}
