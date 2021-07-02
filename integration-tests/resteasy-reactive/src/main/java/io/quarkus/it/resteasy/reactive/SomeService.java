package io.quarkus.it.resteasy.reactive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class SomeService {

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newFixedThreadPool(2);
    }

    @PreDestroy
    public void cleanup() {
        executor.shutdownNow();
    }

    Uni<String> greeting() {
        return Uni.createFrom().item("hello")
                .emitOn(executor);
    }

    Multi<String> greetingAsMulti() {
        return Multi.createFrom().items("h", "e", "l", "l", "o")
                .groupItems().intoMultis().of(2)
                .onItem().transformToUniAndConcatenate(g -> g.collect().in(StringBuffer::new, StringBuffer::append))
                .emitOn(executor)
                .onItem().transform(StringBuffer::toString);
    }

    Uni<Pet> getPet() {
        return Uni.createFrom().item(new Pet().setName("neo").setKind("rabbit"))
                .emitOn(executor);
    }

    public Multi<Pet> getPets() {
        return Multi.createFrom().items(
                new Pet().setName("neo").setKind("rabbit"),
                new Pet().setName("indy").setKind("dog"))
                .emitOn(executor);
    }

    public Multi<Pet> getMorePets() {
        return Multi.createFrom().items(
                new Pet().setName("neo").setKind("rabbit"),
                new Pet().setName("indy").setKind("dog"),
                new Pet().setName("plume").setKind("dog"),
                new Pet().setName("titi").setKind("bird"),
                new Pet().setName("rex").setKind("mouse"))
                .emitOn(executor);
    }
}
