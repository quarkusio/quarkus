package io.quarkus.smallrye.reactivemessaging.hotreload;

import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class SomeSource {

    @Outgoing("my-source")
    public Multi<Integer> source() {
        return Multi.createFrom().items(0, 1, 2, 3, 4, 5, 6, 7, 8)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer l) {
                        return l + 1;
                    }
                });
    }

}
