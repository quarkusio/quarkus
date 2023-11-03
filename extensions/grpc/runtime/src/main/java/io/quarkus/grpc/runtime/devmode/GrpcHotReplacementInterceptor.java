package io.quarkus.grpc.runtime.devmode;

import java.util.function.Supplier;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class GrpcHotReplacementInterceptor implements ServerInterceptor {
    private static volatile Supplier<Boolean> interceptorAction;

    static void register(Supplier<Boolean> onCall) {
        interceptorAction = onCall;
    }

    public static boolean fire() {
        return interceptorAction.get();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        fire();
        return next.startCall(call, headers);
    }
}
