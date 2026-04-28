package io.quarkus.signals.runtime.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import org.jboss.logging.Logger;

import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.Receiver;
import io.quarkus.signals.spi.SignalMetadataEnricher;
import io.smallrye.mutiny.Uni;

class SignalImpl<T> implements Signal<T> {

    private static final Logger LOG = Logger.getLogger(SignalImpl.class);

    private final Type signalType;
    private final Set<Annotation> qualifiers;
    private final Map<String, Object> metadata;
    private final ReceiverManager manager;
    private final ReactiveEmission<T> emission;

    SignalImpl(Type signalType, Set<Annotation> qualifiers, Map<String, Object> metadata, ReceiverManager manager) {
        this.signalType = signalType;
        this.qualifiers = qualifiers;
        this.metadata = metadata;
        this.manager = manager;
        this.emission = new ReactiveEmissionImpl<>();
    }

    @Override
    public Signal<T> select(Annotation... qualifiers) {
        Set<Annotation> mergedQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergedQualifiers, qualifiers);
        return new SignalImpl<>(signalType, mergedQualifiers, metadata, manager);
    }

    @Override
    public <U extends T> Signal<U> select(Class<U> subtype, Annotation... qualifiers) {
        Set<Annotation> mergedQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergedQualifiers, qualifiers);
        return new SignalImpl<>(subtype, mergedQualifiers, metadata, manager);
    }

    @Override
    public <U extends T> Signal<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        Set<Annotation> mergedQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(mergedQualifiers, qualifiers);
        return new SignalImpl<>(subtype.getType(), mergedQualifiers, metadata, manager);
    }

    @Override
    public Signal<T> putMetadata(String key, Object value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        Map<String, Object> meta;
        if (metadata.isEmpty()) {
            meta = Map.of(key, value);
        } else {
            meta = new HashMap<>(metadata);
            meta.put(key, value);
            meta = Map.copyOf(meta);
        }
        return new SignalImpl<>(signalType, qualifiers, meta, manager);
    }

    @Override
    public Signal<T> setMetadata(Map<String, Object> metadata) {
        Objects.requireNonNull(metadata);
        return new SignalImpl<>(signalType, qualifiers, Map.copyOf(metadata), manager);
    }

    @Override
    public ReactiveEmission<T> reactive() {
        return emission;
    }

    @Override
    public void publish(T signal) {
        emission.publish(signal).subscribe().with(NOOP_VOID_ITEM, NOOP_FAILURE);
    }

    @Override
    public void send(T signal) {
        emission.send(signal).subscribe().with(NOOP_VOID_ITEM, NOOP_FAILURE);
    }

    @Override
    public <R> R request(T signal, Class<R> responseType) {
        return emission.request(signal, responseType).await().indefinitely();
    }

    @Override
    public <R> R request(T signal, TypeLiteral<R> responseType) {
        return emission.request(signal, responseType).await().indefinitely();
    }

    private static final Consumer<Void> NOOP_VOID_ITEM = new Consumer<Void>() {

        @Override
        public void accept(Void t) {
            // noop
        }
    };

    private static final Consumer<Throwable> NOOP_FAILURE = new Consumer<Throwable>() {

        @Override
        public void accept(Throwable t) {
            // noop
        }
    };

    private <SIGNAL> SignalContextImpl<SIGNAL> enrich(SIGNAL signal, SignalContext.EmissionType emissionType,
            Type responseType) {
        List<SignalMetadataEnricher> enrichers = manager.enrichers();
        if (enrichers.isEmpty()) {
            return new SignalContextImpl<>(signal, metadata, emissionType, responseType);
        }
        SignalContextImpl<SIGNAL> signalContext = new SignalContextImpl<>(signal, metadata, emissionType, responseType);
        for (SignalMetadataEnricher enricher : enrichers) {
            EnrichmentContextImpl enrichmentContext = new EnrichmentContextImpl(signalContext);
            enricher.enrich(enrichmentContext);
            Map<String, Object> additions = enrichmentContext.additions();
            if (!additions.isEmpty()) {
                Map<String, Object> merged = new HashMap<>(signalContext.metadata());
                merged.putAll(additions);
                signalContext = new SignalContextImpl<>(signal, Map.copyOf(merged), emissionType, responseType);
            }
        }
        return signalContext;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object obj) {
        return (T) obj;
    }

    class ReactiveEmissionImpl<S> implements ReactiveEmission<S> {

        @Override
        public Uni<Void> publish(S signal) {
            List<Receiver<?, ?>> receivers = manager.resolveReceivers(signalType, qualifiers);
            if (receivers.isEmpty()) {
                return Uni.createFrom().voidItem();
            }
            return Uni.createFrom().deferred(new Supplier<Uni<? extends Void>>() {
                @Override
                public Uni<Void> get() {
                    var signalContext = enrich(signal, SignalContext.EmissionType.PUBLISH, null);
                    List<Uni<Object>> unis = new ArrayList<>(receivers.size());
                    for (Receiver<?, ?> receiver : receivers) {
                        unis.add(manager.executeReceiver(cast(receiver), signalContext));
                    }
                    return Uni.join().all(unis).andCollectFailures().replaceWithVoid();
                }
            });
        }

        @Override
        public Uni<Void> send(S signal) {
            var receiver = manager.nextReceiver(signalType, qualifiers, null);
            if (receiver != null) {
                return Uni.createFrom().deferred(new Supplier<Uni<? extends Void>>() {
                    @Override
                    public Uni<Void> get() {
                        var signalContext = enrich(signal, SignalContext.EmissionType.SEND, null);
                        return manager.executeReceiver(cast(receiver), signalContext)
                                .replaceWithVoid();
                    }
                });
            } else {
                return Uni.createFrom().voidItem();
            }
        }

        @Override
        public <R> Uni<R> request(S signal, Class<R> responseType) {
            return request(signal, (Type) responseType);
        }

        @Override
        public <R> Uni<R> request(S signal, TypeLiteral<R> responseType) {
            return request(signal, responseType.getType());
        }

        private <R> Uni<R> request(S signal, Type responseType) {
            var receiver = manager.nextReceiver(signalType, qualifiers, responseType);
            if (receiver != null) {
                return Uni.createFrom().deferred(new Supplier<Uni<? extends R>>() {
                    @Override
                    public Uni<R> get() {
                        var signalContext = enrich(signal, SignalContext.EmissionType.REQUEST, responseType);
                        return cast(manager.executeReceiver(cast(receiver), signalContext));
                    }
                });
            } else {
                LOG.debugf("No receiver matches signal type [%s], qualifiers %s and response type [%s]", signalType, qualifiers,
                        responseType);
                return Uni.createFrom().nullItem();
            }
        }

    }

    static class EnrichmentContextImpl implements SignalMetadataEnricher.EnrichmentContext {

        private final SignalContext<?> signalContext;
        private Map<String, Object> additions;

        EnrichmentContextImpl(SignalContext<?> signalContext) {
            this.signalContext = signalContext;
        }

        @Override
        public SignalContext<?> signalContext() {
            return signalContext;
        }

        @Override
        public void putMetadata(String key, Object value) {
            if (signalContext.metadata().containsKey(key)) {
                throw new IllegalArgumentException("Metadata key already exists: " + key);
            }
            if (additions == null) {
                additions = new HashMap<>();
            }
            if (additions.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Metadata key already exists: " + key);
            }
        }

        Map<String, Object> additions() {
            return additions != null ? additions : Map.of();
        }
    }

    class SignalContextImpl<SIGNAL> implements SignalContext<SIGNAL> {

        private final SIGNAL signal;
        private final Map<String, Object> meta;
        private final SignalContext.EmissionType emissionType;
        private final Type responseType;

        SignalContextImpl(SIGNAL signal, Map<String, Object> meta, SignalContext.EmissionType emissionType, Type responseType) {
            this.signal = signal;
            this.meta = meta;
            this.emissionType = emissionType;
            this.responseType = responseType;
        }

        @Override
        public Map<String, Object> metadata() {
            return meta;
        }

        @Override
        public SIGNAL signal() {
            return signal;
        }

        @Override
        public Type signalType() {
            return signalType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public Set<Annotation> qualifiers() {
            return qualifiers;
        }

        @Override
        public SignalContext.EmissionType emissionType() {
            return emissionType;
        }

        @Override
        public String toString() {
            return "SignalContextImpl [signal=" + signal + ", emissionType=" + emissionType + ", responseType=" + responseType
                    + "]";
        }

    }

}
