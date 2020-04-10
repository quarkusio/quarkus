package io.quarkus.funqy.test;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import io.quarkus.funqy.Funq;

public class GreetingFunctions {

    public static String ERR_MSG = "Identity cannot be null.";

    @Inject
    GreetingService service;

    @Funq
    public Greeting greet(Identity name) {
        if (name == null) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        String message = service.hello(name.getName());
        Greeting greeting = new Greeting();
        greeting.setMessage(message);
        greeting.setName(name.getName());
        return greeting;
    }

    @Funq
    public CompletionStage<Greeting> greetAsync(Identity name) {
        CompletableFuture<Greeting> result = new CompletableFuture<>();
        if (name == null) {
            result.completeExceptionally(new IllegalArgumentException(ERR_MSG));
            return result;
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String message = service.hello(name.getName());
                Greeting greeting = new Greeting();
                greeting.setMessage(message);
                greeting.setName(name.getName());
                result.complete(greeting);
                timer.cancel();
            }
        }, Duration.ofMillis(1).toMillis());

        return result;
    }

}
