package io.quarkus.smallrye.reactivemessaging.mutiny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class MultiIntBean {
    public static final String INT_STREAM = "number-producer";
    public static final String EVEN_STREAM = "even-numbers-producer";

    final List<Integer> evenNumbers = Collections.synchronizedList(new ArrayList<>());

    @Outgoing(INT_STREAM)
    public Multi<Integer> produceInts() {
        return Multi.createFrom().range(1, 7);
    }

    @Incoming(INT_STREAM)
    @Outgoing(EVEN_STREAM)
    public Multi<Integer> timesTwo(Multi<Integer> input) {
        return input.map(i -> i * 2);
    }

    @Incoming(EVEN_STREAM)
    public void collect(Integer input) {
        evenNumbers.add(input);
    }

    public List<Integer> getEvenNumbers() {
        return evenNumbers;
    }
}
