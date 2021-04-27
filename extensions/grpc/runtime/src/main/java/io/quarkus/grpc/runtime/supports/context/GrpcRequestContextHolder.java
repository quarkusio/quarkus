package io.quarkus.grpc.runtime.supports.context;

import io.quarkus.arc.InjectableContext;
import io.vertx.core.Context;

public class GrpcRequestContextHolder {

    private static final String GRPC_REQUEST_CONTEXT_STATE = "GRPC_REQUEST_CONTEXT_STATE";

    volatile InjectableContext.ContextState state;

    public static GrpcRequestContextHolder initialize(Context vertxContext) {
        GrpcRequestContextHolder contextHolder = new GrpcRequestContextHolder();
        vertxContext.put(GRPC_REQUEST_CONTEXT_STATE, contextHolder);
        return contextHolder;
    }

    public static GrpcRequestContextHolder get(Context vertxContext) {
        return vertxContext.get(GRPC_REQUEST_CONTEXT_STATE);
    }
}
