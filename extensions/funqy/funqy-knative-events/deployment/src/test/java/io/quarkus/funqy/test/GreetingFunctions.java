package io.quarkus.funqy.test;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

import jakarta.inject.Inject;

import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class GreetingFunctions {
    @Inject
    GreetingService service;

    @Funq
    public Greeting greet(Identity name) {
        if (name == null) {
            throw new IllegalArgumentException("Identity cannot be null.");
        }
        String message = service.hello(name.getName());
        Greeting greeting = new Greeting();
        greeting.setMessage(message);
        greeting.setName(name.getName());
        return greeting;
    }

    @Funq
    public Uni<Greeting> greetAsync(Identity name) {

        return Uni.createFrom().emitter(uniEmitter -> {
            if (name == null) {
                uniEmitter.fail(new IllegalArgumentException("Identity cannot be null."));
            }
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String message = service.hello(name.getName());
                    Greeting greeting = new Greeting();
                    greeting.setMessage(message);
                    greeting.setName(name.getName());
                    uniEmitter.complete(greeting);
                    timer.cancel();
                }
            }, Duration.ofMillis(1).toMillis());
        });

    }

}
