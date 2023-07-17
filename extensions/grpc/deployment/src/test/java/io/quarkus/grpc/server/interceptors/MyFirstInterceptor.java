package io.quarkus.grpc.server.interceptors;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;

@ApplicationScoped
@GlobalInterceptor
public class MyFirstInterceptor implements ServerInterceptor, Prioritized {

    public static Context.Key<String> KEY_1 = Context.key("X-TEST_1");
    public static Context.Key<Integer> KEY_2 = Context.keyWithDefault("X-TEST_2", -1);
    private volatile long callTime;

    private AtomicInteger counter = new AtomicInteger();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        Context ctx = Context.current().withValue(KEY_1, "k1").withValue(KEY_2, counter.incrementAndGet());
        return Contexts.interceptCall(ctx, new ForwardingServerCall.SimpleForwardingServerCall<>(serverCall) {

            @Override
            public void close(Status status, Metadata trailers) {
                callTime = System.nanoTime();
                super.close(status, trailers);
            }
        }, metadata, serverCallHandler);

    }

    public long getLastCall() {
        return callTime;
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
