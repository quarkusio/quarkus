package com.example.reactive;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@GrpcService
public class ReactiveService implements ReactiveTest {

    BroadcastProcessor<Item> broadcast = BroadcastProcessor.create();

    @Inject
    ContextChecker contextChecker;

    ManagedContext requestContext;

    @PostConstruct
    public void setUp() {
        requestContext = Arc.container().requestContext();
    }

    @Override
    public Uni<Test.Empty> add(Test.Item request) {
        contextChecker.newContextId("ReactiveService#add");
        return Panache.withTransaction(() -> {
            Item newItem = new Item();
            newItem.text = request.getText();
            return Item.persist(newItem)
                    .replaceWith(newItem);
        }).onItem().invoke(newItem -> broadcast.onNext(newItem))
                .replaceWith(Test.Empty.getDefaultInstance());
    }

    @Override
    public Multi<Test.Item> watch(Test.Empty request) {
        int contextId = contextChecker.newContextId("ReactiveService#watch");
        Multi<Item> newItems = broadcast.cache();
        Multi<Test.Item> existing = Item.<Item> streamAll()
                .map(item -> Test.Item.newBuilder().setText(item.text).build());
        return Multi.createBy().concatenating()
                .streams(existing, newItems.map(i -> i.text)
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
