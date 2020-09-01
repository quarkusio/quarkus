package io.quarkus.rest.runtime.jaxrs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

public class QuarkusRestSseBroadcasterImpl implements SseBroadcaster {

    private List<SseEventSink> sinks = new ArrayList<>();

    @Override
    public void onError(BiConsumer<SseEventSink, Throwable> onError) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onClose(Consumer<SseEventSink> onClose) {
        // TODO Auto-generated method stub
    }

    @Override
    public void register(SseEventSink sseEventSink) {
        sinks.add(sseEventSink);
    }

    @Override
    public CompletionStage<?> broadcast(OutboundSseEvent event) {
        CompletableFuture<?>[] cfs = new CompletableFuture[sinks.size()];
        for (int i = 0; i < sinks.size(); i++) {
            SseEventSink sseEventSink = sinks.get(i);
            cfs[i] = sseEventSink.send(event).toCompletableFuture();
        }
        return CompletableFuture.allOf(cfs);
    }

    @Override
    public void close() {
        for (SseEventSink sink : sinks) {
            sink.close();
        }
    }

}
