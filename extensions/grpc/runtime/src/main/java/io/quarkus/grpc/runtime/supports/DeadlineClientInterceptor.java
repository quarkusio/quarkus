package io.quarkus.grpc.runtime.supports;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

/**
 * Applies the configured deadline to each call when it starts. A deadline set on the stub itself is an absolute
 * point in time computed when the stub is created, so it would be shared by every call made through the stub.
 * A call that already carries a deadline keeps it.
 */
public class DeadlineClientInterceptor implements ClientInterceptor {

    private final long deadlineMillis;

    public DeadlineClientInterceptor(Duration deadline) {
        this.deadlineMillis = deadline.toMillis();
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
            Channel next) {
        if (callOptions.getDeadline() == null) {
            callOptions = callOptions.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS);
        }
        return next.newCall(method, callOptions);
    }
}
