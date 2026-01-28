package io.quarkus.infinispan.client.runtime.jfr;

import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.ExecutionMode.ASYNC;
import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.Scope.CACHE_WIDE;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.infinispan.client.runtime.jfr.event.InfinispanEventRecorder;
import io.quarkus.jfr.api.IdProducer;

@JfrCacheOperation(executionMode = ASYNC, scope = CACHE_WIDE)
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class AsyncCacheWideInterceptor extends AbstractCacheOperationInterceptor {

    @Override
    InfinispanEventRecorder createEventRecorder(InvocationContext context, IdProducer idProducer) {
        return super.createCacheWideEventRecorder(context, idProducer);
    }

    @Override
    Object invoke(InvocationContext context, InfinispanEventRecorder eventRecorder) throws Exception {
        return invokeAsync(context, eventRecorder);
    }
}
