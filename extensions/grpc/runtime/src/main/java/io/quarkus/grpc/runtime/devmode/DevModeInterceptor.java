package io.quarkus.grpc.runtime.devmode;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class DevModeInterceptor implements ServerInterceptor {
    private final ClassLoader classLoader;

    public DevModeInterceptor(ClassLoader contextClassLoader) {
        classLoader = contextClassLoader;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
            ServerCallHandler<ReqT, RespT> next) {
        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return next.startCall(serverCall, metadata);
        } finally {
            Thread.currentThread().setContextClassLoader(originalTccl);
        }
    }
}
