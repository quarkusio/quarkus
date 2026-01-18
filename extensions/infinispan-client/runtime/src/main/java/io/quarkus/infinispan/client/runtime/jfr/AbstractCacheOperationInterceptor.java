package io.quarkus.infinispan.client.runtime.jfr;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.infinispan.client.hotrod.RemoteCache;

import io.quarkus.infinispan.client.runtime.jfr.event.InfinispanEventRecorder;
import io.quarkus.jfr.api.IdProducer;

public abstract class AbstractCacheOperationInterceptor {

    @Inject
    IdProducer idProducer;

    @AroundInvoke
    Object aroundInvoke(InvocationContext context) throws Exception {
        InfinispanEventRecorder eventRecorder = createEventRecorder(context, idProducer);
        eventRecorder.createAndCommitStartEvent().createPeriodEvent();
        return invoke(context, eventRecorder);
    }

    abstract InfinispanEventRecorder createEventRecorder(InvocationContext context, IdProducer idProducer);

    abstract Object invoke(InvocationContext context, InfinispanEventRecorder eventRecorder) throws Exception;

    protected InfinispanEventRecorder createSingleEntryEventRecorder(InvocationContext context, IdProducer idProducer) {
        String traceId = idProducer.getTraceId();
        String spanId = idProducer.getSpanId();
        RemoteCache remoteCache = (RemoteCache) context.getTarget();
        String cacheName = remoteCache.getName();
        String clusterName = remoteCache.getRemoteCacheContainer().getCurrentClusterName();
        Method method = context.getMethod();
        String methodName = method.getName();
        return InfinispanEventRecorder.createSingleEntryEventRecorder(traceId, spanId, methodName, cacheName, clusterName);
    }

    protected InfinispanEventRecorder createMultiEntryEventRecorder(InvocationContext context, IdProducer idProducer) {
        String traceId = idProducer.getTraceId();
        String spanId = idProducer.getSpanId();
        RemoteCache remoteCache = (RemoteCache) context.getTarget();
        String cacheName = remoteCache.getName();
        String clusterName = remoteCache.getRemoteCacheContainer().getCurrentClusterName();
        Method method = context.getMethod();
        String methodName = method.getName();
        Collection allParams = (Collection) context.getParameters()[0];
        int size = allParams.size();
        return InfinispanEventRecorder.createMultiEntryEventRecorder(traceId, spanId, methodName, cacheName, clusterName, size);
    }

    protected InfinispanEventRecorder createCacheWideEventRecorder(InvocationContext context, IdProducer idProducer) {
        String traceId = idProducer.getTraceId();
        String spanId = idProducer.getSpanId();
        RemoteCache remoteCache = (RemoteCache) context.getTarget();
        String cacheName = remoteCache.getName();
        String clusterName = remoteCache.getRemoteCacheContainer().getCurrentClusterName();
        Method method = context.getMethod();
        String methodName = method.getName();
        return InfinispanEventRecorder.createCacheWideEventRecorder(traceId, spanId, methodName, cacheName, clusterName);
    }

    protected Object invokeSync(InvocationContext context, InfinispanEventRecorder eventRecorder) throws Exception {
        try {
            return context.proceed();
        } finally {
            eventRecorder.commitPeriodEvent().createAndCommitEndEvent();
        }
    }

    protected Object invokeAsync(InvocationContext context, InfinispanEventRecorder eventRecorder) throws Exception {
        return ((CompletableFuture<?>) (context.proceed())).whenComplete(new AsyncEventEndHandler(eventRecorder));
    }

    record AsyncEventEndHandler(InfinispanEventRecorder eventRecorder) implements BiConsumer<Object, Throwable> {

        @Override
        public void accept(Object result, Throwable throwable) {
            eventRecorder.commitPeriodEvent().createAndCommitEndEvent();

            if (throwable != null) {
                throw new RuntimeException(throwable);
            }
        }
    }
}
