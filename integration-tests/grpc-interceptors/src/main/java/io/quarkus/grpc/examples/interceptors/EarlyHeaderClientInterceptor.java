package io.quarkus.grpc.examples.interceptors;

import java.util.concurrent.CompletableFuture;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

public class EarlyHeaderClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> HEADER = Metadata.Key.of("xx-acme-header", Metadata.ASCII_STRING_MARSHALLER);
    private final CompletableFuture<String> headerFuture = new CompletableFuture<>();

    public CompletableFuture<String> getHeaderFuture() {
        return headerFuture;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor,
            CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        headerFuture.complete(headers.get(HEADER));
                        super.onHeaders(headers);
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        if (!headerFuture.isDone()) {
                            headerFuture.completeExceptionally(new RuntimeException("Call closed before receiving headers"));
                        }
                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }
}
