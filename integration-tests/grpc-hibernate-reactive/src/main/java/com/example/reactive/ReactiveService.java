package com.example.reactive;

import java.util.Objects;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@GrpcService
public class ReactiveService implements ReactiveTest {

    BroadcastProcessor<Item> broadcast = BroadcastProcessor.create();

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    ContextChecker contextChecker;

    ManagedContext requestContext;

    @PostConstruct
    public void setUp() {
        requestContext = Arc.container().requestContext();
    }

    @Override
    public Uni<Test.Empty> add(Test.Item request) {
        assert VertxContext.isOnDuplicatedContext();
        int contextId = contextChecker.newContextId("ReactiveService#add");
        return sessionFactory.withTransaction(session -> {
            Item newItem = new Item();
            newItem.text = request.getText();
            if (!Objects.equals(ContextLocals.get("context-id").get(), contextId)) {
                throw new RuntimeException("Different context on `ContextLocals` and `ReactiveService#add` method");
            }
            return session.persist(newItem)
                    .replaceWith(newItem);
        }).onItem().invoke(newItem -> broadcast.onNext(newItem))
                .replaceWith(Test.Empty.getDefaultInstance());
    }

    @Override
    public Multi<Test.Item> watch(Test.Empty request) {
        int contextId = contextChecker.newContextId("ReactiveService#watch");
        Multi<Item> cached = broadcast.cache();
        cached.subscribe().with(i -> {
        });
        Multi<Test.Item> existing = Item.<Item> streamAll()
                .map(item -> Test.Item.newBuilder().setText(item.text).build());
        return Multi.createBy().concatenating()
                .streams(existing, cached.map(i -> i.text)
                        .map(Test.Item.newBuilder()::setText)
                        .map(Test.Item.Builder::build))
                .onItem().invoke(
                        () -> {
                            if (contextChecker.requestContextId() != contextId) {
                                throw new RuntimeException("Different context for `onItem` and `ReactiveService#watch` method");
                            }
                            if (!requestContext.isActive()) {
                                throw new RuntimeException(
                                        "Request context not active for `onItem` in `ReactiveService#watch`");
                            }
                        })
                .onCancellation().invoke(() -> System.out.println("canceled"));
    }
}
