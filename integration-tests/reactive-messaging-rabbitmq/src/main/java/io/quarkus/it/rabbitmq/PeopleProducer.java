package io.quarkus.it.rabbitmq;

import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;

@ApplicationScoped
public class PeopleProducer {
    @Outgoing("people-out")
    public Multi<Person> generatePeople() {
        System.out.println("---- Calling generatePeople");
        return Multi.createFrom().emitter(new Consumer<MultiEmitter<? super Person>>() {
            @Override
            public void accept(MultiEmitter<? super Person> multiEmitter) {
                System.out.println("---- Calling accept called, multi has been subscribed");
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    multiEmitter.emit(new Person("bob"));
                    multiEmitter.emit(new Person("alice"));
                    multiEmitter.emit(new Person("tom"));
                    multiEmitter.emit(new Person("jerry"));
                    multiEmitter.emit(new Person("anna"));
                    multiEmitter.emit(new Person("ken"));

                }).start();
            }
        });
    }
}
