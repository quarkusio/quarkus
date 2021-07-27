package io.quarkus.grpc.runtime.supports;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.vertx.core.Context;

public class EventLoopBlockingCheckInterceptor implements ClientInterceptor {
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
            Channel next) {
        if (Context.isOnEventLoopThread()) {
            throw new IllegalStateException("Blocking gRPC client call made from the event loop. " +
                    "If the code is executed from a gRPC service or a RESTEasy Reactive resource, either annotate the method " +
                    " that makes the call with `@Blocking` or use the non-blocking client.");
        }
        return next.newCall(method, callOptions);
    }
}
