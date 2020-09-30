package io.quarkus.grpc.runtime.supports;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import io.grpc.*;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class BlockingServerInterceptor implements ServerInterceptor {

    private final Vertx vertx;

    public BlockingServerInterceptor(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        ReplayListener<ReqT> replay = new ReplayListener<>();

        vertx.executeBlocking(new Handler<Promise<Object>>() {
            @Override
            public void handle(Promise<Object> f) {
                ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
                replay.setDelegate(listener);
                f.complete(null);
            }
        }, null);

        return replay;
    }

    /**
     * Stores the incoming events until the listener is injected.
     * When injected, replay the events.
     *
     * Note that event must be executed in order, explaining the `ordered:true`.
     */
    private class ReplayListener<ReqT> extends ServerCall.Listener<ReqT> {
        private ServerCall.Listener<ReqT> delegate;
        private final List<Consumer<ServerCall.Listener<ReqT>>> incomingEvents = new LinkedList<>();

        synchronized void setDelegate(ServerCall.Listener<ReqT> delegate) {
            this.delegate = delegate;
            for (Consumer<ServerCall.Listener<ReqT>> event : incomingEvents) {
                event.accept(delegate);
            }
            incomingEvents.clear();
        }

        private synchronized void executeOnContextOrEnqueue(Consumer<ServerCall.Listener<ReqT>> consumer) {
            if (this.delegate != null) {
                vertx.executeBlocking(new Handler<Promise<Object>>() {
                    @Override
                    public void handle(Promise<Object> f) {
                        consumer.accept(delegate);
                        f.complete();
                    }
                }, true, null);
            } else {
                incomingEvents.add(consumer);
            }
        }

        @Override
        public void onMessage(ReqT message) {
            executeOnContextOrEnqueue(new Consumer<ServerCall.Listener<ReqT>>() {
                @Override
                public void accept(ServerCall.Listener<ReqT> t) {
                    t.onMessage(message);
                }
            });
        }

        @Override
        public void onHalfClose() {
            executeOnContextOrEnqueue(ServerCall.Listener::onHalfClose);
        }

        @Override
        public void onCancel() {
            executeOnContextOrEnqueue(ServerCall.Listener::onCancel);
        }

        @Override
        public void onComplete() {
            executeOnContextOrEnqueue(ServerCall.Listener::onComplete);
        }

        @Override
        public void onReady() {
            executeOnContextOrEnqueue(ServerCall.Listener::onReady);
        }
    }

}
