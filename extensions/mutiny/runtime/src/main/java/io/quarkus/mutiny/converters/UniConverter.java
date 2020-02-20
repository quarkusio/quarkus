package io.quarkus.mutiny.converters;

import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

@SuppressWarnings("rawtypes")
public class UniConverter implements ReactiveTypeConverter<Uni> {

    @SuppressWarnings("unchecked")
    @Override
    public <X> CompletionStage<X> toCompletionStage(Uni instance) {
        return instance.subscribeAsCompletionStage();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> Publisher<X> toRSPublisher(Uni instance) {
        return instance.toMulti();
    }

    @Override
    public <X> Uni fromCompletionStage(CompletionStage<X> cs) {
        return Uni.createFrom().completionStage(cs);
    }

    @Override
    public <X> Uni fromPublisher(Publisher<X> publisher) {
        return Uni.createFrom().publisher(publisher);
    }

    @Override
    public Class<Uni> type() {
        return Uni.class;
    }

    @Override
    public boolean emitItems() {
        return true;
    }

    @Override
    public boolean emitAtMostOneItem() {
        return true;
    }

    @Override
    public boolean supportNullValue() {
        return true;
    }
}
