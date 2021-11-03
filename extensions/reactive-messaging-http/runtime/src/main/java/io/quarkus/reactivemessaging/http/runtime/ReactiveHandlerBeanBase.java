package io.quarkus.reactivemessaging.http.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import io.quarkus.reactivemessaging.http.runtime.config.StreamConfigBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.ext.web.RoutingContext;

abstract class ReactiveHandlerBeanBase<ConfigType extends StreamConfigBase, MessageType> {

    protected final Map<String, Bundle<MessageType>> processors = new HashMap<>();

    @PostConstruct
    void init() {
        configs().forEach(this::addProcessor);
    }

    void handle(RoutingContext event) {
        Bundle<MessageType> bundle = processors.get(key(event));
        if (bundle != null) {
            MultiEmitter<? super MessageType> emitter = bundle.emitter;
            StrictQueueSizeGuard guard = bundle.guard;
            handleRequest(event, emitter, guard, bundle.path);
        } else {
            event.response().setStatusCode(404).end();
        }
    }

    private void addProcessor(ConfigType streamConfig) {
        StrictQueueSizeGuard guard = new StrictQueueSizeGuard(streamConfig.bufferSize);
        Bundle<MessageType> bundle = new Bundle<>(guard);

        Multi<MessageType> processor = Multi.createFrom()
                // emitter with an unbounded queue, we control the size ourselves, with the guard
                .<MessageType> emitter(bundle::setEmitter, BackPressureStrategy.BUFFER)
                .onItem().invoke(guard::dequeue);
        bundle.setProcessor(processor);
        bundle.setPath(streamConfig.path);

        Bundle<MessageType> previousProcessor = processors.put(key(streamConfig), bundle);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for " + description(streamConfig));
        }
    }

    protected abstract void handleRequest(RoutingContext event, MultiEmitter<? super MessageType> emitter,
            StrictQueueSizeGuard guard, String path);

    protected abstract String description(ConfigType streamConfig);

    protected abstract String key(ConfigType streamConfig);

    protected abstract String key(RoutingContext context);

    protected abstract Collection<ConfigType> configs();

    protected class Bundle<MessageType> {
        private final StrictQueueSizeGuard guard;
        private Multi<MessageType> processor; // effectively final
        private MultiEmitter<? super MessageType> emitter; // effectively final
        private String path;

        private Bundle(StrictQueueSizeGuard guard) {
            this.guard = guard;
        }

        public void setProcessor(Multi<MessageType> processor) {
            this.processor = processor;
        }

        public void setEmitter(MultiEmitter<? super MessageType> emitter) {
            this.emitter = emitter;
        }

        public Multi<MessageType> getProcessor() {
            return processor;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
