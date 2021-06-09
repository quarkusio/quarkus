package io.quarkus.grpc.runtime.supports.context;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Interceptor
@GrpcEnableRequestContext
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class GrpcRequestContextCdiInterceptor {

    @AroundInvoke
    public Object cleanUpContext(InvocationContext invocationContext) throws Exception {
        boolean cleanUp = false;
        ManagedContext requestContext = Arc.container().requestContext();
        if (!requestContext.isActive()) {
            Context context = Vertx.currentContext();

            if (context != null) {
                cleanUp = true;
                requestContext.activate();
                GrpcRequestContextHolder contextHolder = GrpcRequestContextHolder.get(context);
                if (contextHolder != null) {
                    contextHolder.state = requestContext.getState();
                }
            }
        }
        try {
            return invocationContext.proceed();
        } finally {
            if (cleanUp) {
                requestContext.deactivate();
            }
        }
    }
}
