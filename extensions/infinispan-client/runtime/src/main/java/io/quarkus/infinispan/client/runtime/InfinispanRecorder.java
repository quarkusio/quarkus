package io.quarkus.infinispan.client.runtime;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.AnnotationLiteral;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.counter.api.CounterManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RelaxedValidation;

@Recorder
public class InfinispanRecorder {

    public BeanContainerListener configureInfinispan(@RelaxedValidation Map<String, Properties> properties) {
        return container -> {
            InfinispanClientProducer instance = container.beanInstance(InfinispanClientProducer.class);
            instance.setProperties(properties);
        };
    }

    private final Set<String> clientNames = new HashSet<>();

    public Supplier<RemoteCacheManager> infinispanRemoteCacheManagerSupplier(String clientName) {
        clientNames.add(clientName);
        return new InfinispanClientSupplier<>(new Function<>() {
            @Override
            public RemoteCacheManager apply(InfinispanClientProducer infinispanClientProducer) {
                return infinispanClientProducer.getNamedRemoteCacheManager(clientName);
            }
        });
    }

    public Supplier<CounterManager> infinispanCounterManagerSupplier(String clientName) {
        clientNames.add(clientName);
        return new InfinispanClientSupplier<>(new Function<>() {
            @Override
            public CounterManager apply(InfinispanClientProducer infinispanClientProducer) {
                return infinispanClientProducer.getNamedCounterManager(clientName);
            }
        });
    }

    public <K, V> Supplier<RemoteCache<K, V>> infinispanRemoteCacheSupplier(String clientName, String cacheName) {
        clientNames.add(clientName);
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
}
