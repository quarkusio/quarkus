package io.quarkus.it.amqp;

import java.time.Duration;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.json.Json;

@ApplicationScoped
public class PeopleProducer {

    @Outgoing("people-out")
    public Multi<String> generatePeople() {
        //TODO: this can be replaced with Multi.onItem().delayIt when it exists
        //TODO: this delay should not even be necessary, the queue is created on
        //subscriber connect, so we delay to make sure it is connected
        //we should be able to just define the queue in broker.xml, but that does not
        //work atm, see https://github.com/smallrye/smallrye-reactive-messaging/issues/555
        return Multi.createFrom().emitter(new Consumer<MultiEmitter<? super Person>>() {
            @Override
            public void accept(MultiEmitter<? super Person> multiEmitter) {
                Uni.createFrom().item("dummy").onItem().delayIt().by(Duration.ofSeconds(2))
                        .subscribe().with(new Consumer<String>() {
                            @Override
                            public void accept(String s) {
                                multiEmitter.emit(new Person("bob"));
                                multiEmitter.emit(new Person("alice"));
                                multiEmitter.emit(new Person("tom"));
                                multiEmitter.emit(new Person("jerry"));
                                multiEmitter.emit(new Person("anna"));
                                multiEmitter.emit(new Person("ken"));
                            }
                        });
            }
        }).map(Json::encode);
    }
}
