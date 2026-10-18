package io.quarkus.mutiny.reactive.operators.deployment;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

@Path("/test")
public class MyTestResource {

    @GET
    public CompletionStage<String> compute() {
        return ReactiveStreams.of(1, 2, 3)
                .map(i -> i + 1) // 2, 3, 4
                .flatMapRsPublisher(x -> ReactiveStreams.of(x, x).buildRs()) // 2,2,3,3,4,4
                .distinct() // 2, 3, 4
                .limit(2) // 2, 3
                .collect(AtomicInteger::new, AtomicInteger::addAndGet)// 5
                .run()
                .thenApply(i -> Integer.toString(i.get())); // "5"
    }

}
