package io.quarkus.netty.runtime.virtual;

import java.util.concurrent.CompletableFuture;

public class VirtualMessage {
    private Object message;
    private CompletableFuture<Void> future = new CompletableFuture<>();

    public VirtualMessage(Object message) {
        this.message = message;
    }

    public Object getMessage() {
        return message;
    }

    public void completed() {
        future.complete(null);
    }

    public void awaitComplete() throws Exception {
        future.get();
    }
}
