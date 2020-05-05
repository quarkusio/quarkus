package io.quarkus.smallrye.reactivemessaging.hotreload;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.reactivex.Flowable;

@ApplicationScoped
public class SomeSource {

    @Outgoing("my-source")
    public Flowable<Integer> source() {
        return Flowable.just(0, 1, 2, 3, 4, 5, 6, 7, 8)
                .map(l -> l + 1);
    }

}
