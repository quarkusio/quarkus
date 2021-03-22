package io.quarkus.mongodb.impl;

import java.util.List;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

class Wrappers {

    private Wrappers() {
        // Avoid direct instantiation.
    }

    static <T> Uni<T> toUni(Publisher<T> publisher) {
        Context context = Vertx.currentContext();
        Uni<T> uni = Uni.createFrom().publisher(publisher);
        if (context != null) {
            return uni.emitOn(command -> context.runOnContext(x -> command.run()));
        }
        return uni;
    }

    static <T> Multi<T> toMulti(Publisher<T> publisher) {
        Context context = Vertx.currentContext();
        if (context != null) {
            return Multi.createFrom().publisher(publisher).emitOn(command -> context.runOnContext(x -> command.run()));
        } else {
            return Multi.createFrom().publisher(publisher);
        }
    }

    static <T> Uni<List<T>> toUniOfList(Publisher<T> publisher) {
        Context context = Vertx.currentContext();
        Uni<List<T>> uni = Multi.createFrom().publisher(publisher)
                .collect().asList();

        if (context != null) {
            return uni.emitOn(command -> context.runOnContext(x -> command.run()));
        }
        return uni;
    }
}
