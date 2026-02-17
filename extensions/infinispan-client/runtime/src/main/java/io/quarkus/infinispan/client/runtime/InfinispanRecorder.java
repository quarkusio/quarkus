package io.quarkus.infinispan.client.runtime;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.AnnotationLiteral;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.counter.api.CounterManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RelaxedValidation;

@Recorder
public class InfinispanRecorder {

    public BeanContainerListener configureInfinispan(@RelaxedValidation Map<String, Properties> properties) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                InfinispanClientProducer instance = container.beanInstance(InfinispanClientProducer.class);
                instance.setProperties(properties);
            }
        };
    }

    public Supplier<RemoteCacheManager> infinispanRemoteCacheManagerSupplier(String clientName) {
        return new InfinispanClientSupplier<>(new Function<>() {
            @Override
            public RemoteCacheManager apply(InfinispanClientProducer infinispanClientProducer) {
                RemoteCacheManager result = infinispanClientProducer.getNamedRemoteCacheManager(clientName);
                if (result == null) {
                    if (clientName.startsWith(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME)) {
                        throw new IllegalStateException("No configuration found for the default Infinispan client");
                    } else {
                        throw new IllegalStateException("No configuration found for Infinispan client '" + clientName + "'");
                    }
                }
                return result;
            }
        });
    }

    public Supplier<CounterManager> infinispanCounterManagerSupplier(String clientName) {
        return new InfinispanClientSupplier<>(new Function<>() {
            @Override
            public CounterManager apply(InfinispanClientProducer infinispanClientProducer) {
                return infinispanClientProducer.getNamedCounterManager(clientName);
            }
        });
    }

    public <K, V> Supplier<RemoteCache<K, V>> infinispanRemoteCacheSupplier(String clientName, String cacheName) {
        return new InfinispanClientSupplier<>(new Function<>() {
            @Override
            public RemoteCache<K, V> apply(InfinispanClientProducer infinispanClientProducer) {
                return infinispanClientProducer.getRemoteCache(clientName, cacheName);
            }
        });
    }

    public RuntimeValue<RemoteCacheManager> initializeClient(String name) {
        RemoteCacheManager remoteCacheManager = Arc.container().instance(RemoteCacheManager.class, literal(name)).get();
        //noinspection ResultOfMethodCallIgnored
        // used to initialize the bean
        remoteCacheManager.getConfiguration();
        return new RuntimeValue<>(remoteCacheManager);
    }

    @SuppressWarnings("rawtypes")
    private AnnotationLiteral literal(String name) {
        if (name.startsWith(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME)) {
            return Default.Literal.INSTANCE;
        }
        return NamedLiteral.of(name);
    }

    /** Helper to lazily create Infinispan clients. */
    static final class InfinispanClientSupplier<T> implements Supplier<T> {
        private final Function<InfinispanClientProducer, T> producer;

        InfinispanClientSupplier(Function<InfinispanClientProducer, T> producer) {
            this.producer = producer;
        }

        @Override
        public T get() {
            InfinispanClientProducer infinispanClientProducer = Arc.container().instance(InfinispanClientProducer.class).get();
            return producer.apply(infinispanClientProducer);
        }
    }

    public void eagerInitAllCaches() {
        ArcContainer container = Arc.container();
        // match all RemoteCache
        InjectableInstance<RemoteCache> allCaches = container.select(
                RemoteCache.class, Any.Literal.INSTANCE);

        for (InstanceHandle<RemoteCache> handle : allCaches.handles()) {
            handle.get(); // Force init
        }
    }
}
