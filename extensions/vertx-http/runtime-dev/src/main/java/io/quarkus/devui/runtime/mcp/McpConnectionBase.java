package io.quarkus.devui.runtime.mcp;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

abstract class McpConnectionBase implements McpConnection, Sender {

    protected final String id;

    protected final AtomicReference<Status> status;

    protected final AtomicReference<InitialRequest> initializeRequest;

    protected final AtomicReference<McpLog.LogLevel> logLevel;

    protected final TrafficLogger trafficLogger;

    protected final Optional<Duration> autoPingInterval;

    protected final AtomicLong lastUsed;

    protected final long idleTimeout;

    protected McpConnectionBase(String id, McpServerRuntimeConfig config) {
        this.id = id;
        this.status = new AtomicReference<>(Status.NEW);
        this.initializeRequest = new AtomicReference<>();

        //TODO: pass proper values for all these
        this.logLevel = new AtomicReference<>(McpLog.LogLevel.INFO);
        this.trafficLogger = null;
        this.autoPingInterval = Optional.empty();
        this.lastUsed = new AtomicLong(Instant.now().toEpochMilli());
        this.idleTimeout = config.connectionIdleTimeout().toMillis();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Status status() {
        return status.get();
    }

    @Override
    public InitialRequest initialRequest() {
        return initializeRequest.get();
    }

    public boolean initialize(InitialRequest request) {
        if (status.compareAndSet(Status.NEW, Status.INITIALIZING)) {
            initializeRequest.set(request);
            return true;
        }
        return false;
    }

    public boolean close() {
        return status.compareAndSet(Status.IN_OPERATION, Status.CLOSED);
    }

    @Override
    public McpLog.LogLevel logLevel() {
        return logLevel.get();
    }

    void setLogLevel(McpLog.LogLevel level) {
        this.logLevel.set(level);
    }

    public boolean setInitialized() {
        return status.compareAndSet(Status.INITIALIZING, Status.IN_OPERATION);
    }

    public TrafficLogger trafficLogger() {
        return trafficLogger;
    }

    public Optional<Duration> autoPingInterval() {
        return autoPingInterval;
    }

    public McpConnectionBase touch() {
        this.lastUsed.set(Instant.now().toEpochMilli());
        return this;
    }

    public boolean isIdleTimeoutExpired() {
        if (idleTimeout <= 0) {
            return false;
        }
        return Instant.now().minusMillis(lastUsed.get()).toEpochMilli() > idleTimeout;
    }

}
