package io.quarkus.netty.runtime.virtual;

public interface VirtualResponseHandler {
    void handleMessage(Object msg);

    void close();
}
