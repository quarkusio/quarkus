package io.quarkus.infinispan.client.runtime.jfr;

import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.ExecutionMode.ASYNC;
import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.Scope.MULTI;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.infinispan.client.runtime.jfr.event.InfinispanEventRecorder;
import io.quarkus.jfr.api.IdProducer;

@JfrCacheOperation(executionMode = ASYNC, scope = MULTI)
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class AsyncMultiEntryInterceptor extends AbstractCacheOperationInterceptor {

    @Override
    InfinispanEventRecorder createEventRecorder(InvocationContext context, IdProducer idProducer) {
        return createMultiEntryEventRecorder(context, idProducer);
    }

    @Override
    Object invoke(InvocationContext context, InfinispanEventRecorder eventRecorder) throws Exception {
        return invokeAsync(context, eventRecorder);
    }
}
