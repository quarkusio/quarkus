package io.quarkus.infinispan.client.runtime.jfr;

import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.ExecutionMode.SYNC;
import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.Scope.SINGLE;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.infinispan.client.runtime.jfr.event.InfinispanEventRecorder;
import io.quarkus.jfr.api.IdProducer;

@JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class SyncSingleEntryInterceptor extends AbstractCacheOperationInterceptor {

    @Override
    InfinispanEventRecorder createEventRecorder(InvocationContext context, IdProducer idProducer) {
        return super.createSingleEntryEventRecorder(context, idProducer);
    }

    @Override
    Object invoke(InvocationContext context, InfinispanEventRecorder eventRecorder) throws Exception {
        return invokeSync(context, eventRecorder);
    }
}
