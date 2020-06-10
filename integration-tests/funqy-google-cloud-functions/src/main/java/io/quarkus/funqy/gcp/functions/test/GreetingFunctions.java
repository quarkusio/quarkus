package io.quarkus.funqy.gcp.functions.test;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.gcp.functions.event.PubsubMessage;
import io.quarkus.funqy.gcp.functions.event.StorageEvent;
import io.smallrye.mutiny.Uni;

public class GreetingFunctions {

    @Inject
    GreetingService service;

    @Funq
    public Greeting helloHttpWorld() {
        String message = service.hello("world");
        Greeting greeting = new Greeting();
        greeting.setMessage(message);
        greeting.setName("world");
        return greeting;
    }

    @Funq
    public Uni<Greeting> helloHttpWorldAsync() {
        return Uni.createFrom().emitter(uniEmitter -> {

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String message = service.hello("world");
                    Greeting greeting = new Greeting();
                    greeting.setMessage(message);
                    greeting.setName("world");
                    uniEmitter.complete(greeting);
                    timer.cancel();
                }
            }, Duration.ofMillis(1).toMillis());
        });
    }

    @Funq
    public void helloPubSubWorld(PubsubMessage pubSubEvent) {
        String message = service.hello("world");
        System.out.println(pubSubEvent.messageId + " - " + message);
    }

    @Funq
    public void helloGCSWorld(StorageEvent storageEvent) {
        String message = service.hello("world");
        System.out.println(storageEvent.name + " - " + message);
    }

}
