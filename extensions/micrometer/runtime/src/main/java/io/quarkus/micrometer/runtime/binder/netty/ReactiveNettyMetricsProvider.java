package io.quarkus.micrometer.runtime.binder.netty;

import java.lang.reflect.Field;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.netty.buffer.ByteBufAllocatorMetricProvider;

@Singleton
public class ReactiveNettyMetricsProvider {

    @Produces
    @Singleton
    public MeterBinder reactiveAllocatorMetrics() throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Class<?> clazz = tccl.loadClass("org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload");
        Field af = clazz.getDeclaredField("ALLOC");
        af.setAccessible(true);
        ByteBufAllocatorMetricProvider provider = (ByteBufAllocatorMetricProvider) af.get(null);
        return new NettyAllocatorMetrics(provider);
    }

}
