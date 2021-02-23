package io.quarkus.grpc.runtime.supports;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Prioritized;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@ApplicationScoped
public class IOThreadClientInterceptor implements ClientInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {

        Context context = Vertx.currentContext();

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    private volatile CompletableFuture<Void> onMessageCompletion;

                    @Override
                    public void onMessage(RespT message) {
                        if (context != null) {
                            onMessageCompletion = new CompletableFuture<>();
                            context.runOnContext(unused -> {
                                try {
                                    super.onMessage(message);
                                    onMessageCompletion.complete(null);
                                } catch (Throwable any) {
                                    onMessageCompletion.completeExceptionally(any);
                                }
                            });
                        } else {
                            super.onMessage(message);
                        }
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        if (onMessageCompletion != null && !Context.isOnEventLoopThread()) {
                            try {
                                onMessageCompletion.get(60, TimeUnit.SECONDS);
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException("`onMessage` failed or interrupted", e);
                            } catch (TimeoutException e) {
                                throw new RuntimeException("`onMessage` did not complete in 60 seconds");
                            }
                            super.onClose(status, trailers);
                        } else {
                            super.onClose(status, trailers);
                        }
                    }
                }, headers);
            }
        };
    }

    @Override
    public int getPriority() {
        // MAX to be sure to be called first.
        return Integer.MAX_VALUE;
    }
}
