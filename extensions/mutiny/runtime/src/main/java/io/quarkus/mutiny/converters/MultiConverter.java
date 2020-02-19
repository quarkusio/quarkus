package io.quarkus.mutiny.converters;

import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

@SuppressWarnings("rawtypes")
public class MultiConverter implements ReactiveTypeConverter<Multi> {

    @SuppressWarnings("unchecked")
    @Override
    public <X> CompletionStage<X> toCompletionStage(Multi instance) {
        return instance.toUni().subscribeAsCompletionStage();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> Publisher<X> toRSPublisher(Multi instance) {
        return instance;
    }

    @Override
    public <X> Multi fromCompletionStage(CompletionStage<X> cs) {
        return Multi.createFrom().completionStage(cs);
    }

    @Override
    public <X> Multi fromPublisher(Publisher<X> publisher) {
        return Multi.createFrom().publisher(publisher);
    }

    @Override
    public Class<Multi> type() {
        return Multi.class;
    }

    @Override
    public boolean emitItems() {
        return true;
    }

    @Override
    public boolean emitAtMostOneItem() {
        return false;
    }

    @Override
    public boolean supportNullValue() {
        return false;
    }
}
