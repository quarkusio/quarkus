package com.example.grpc.hibernate;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import com.example.test.Test;
import com.example.test.TestOuterClass;

import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class TestService implements Test {

    @Inject
    Logger log;

    private static final TestOuterClass.Empty EMPTY = TestOuterClass.Empty.getDefaultInstance();

    @Inject
    EntityManager entityManager;

    @Inject
    ItemDao dao;

    @Inject
    ContextChecker contextChecker;

    @Override
    @Blocking
    @Transactional
    public Uni<TestOuterClass.Empty> add(TestOuterClass.Item request) {
        contextChecker.newContextId("TestService#add");
        Item item = new Item();
        item.text = request.getText();
        entityManager.persist(item);
        return Uni.createFrom().item(EMPTY);
    }

    @Override
    @Transactional
    public Uni<TestOuterClass.Empty> clear(TestOuterClass.Empty request) {
        contextChecker.newContextId("TestService#clear");
        entityManager.createQuery("DELETE from Item")
                .executeUpdate();
        return Uni.createFrom().item(EMPTY);
    }

    @Override
    @Blocking
    public Multi<TestOuterClass.Item> getAll(TestOuterClass.Empty request) {
        contextChecker.newContextId("TestService#getAll");
        List<Item> items = entityManager.createQuery("from Item", Item.class)
                .getResultList();
        // todo: remove logging
        log.infof("returning items: %s", items);

        return Multi.createFrom().iterable(items)
                .map(i -> TestOuterClass.Item.newBuilder().setText(i.text).build())
                .onItem().invoke(item -> log.infof("emitting %s", item))
                .onCompletion().invoke(() -> log.info("completed emission"));
    }

    @Override
    @Blocking
    public Multi<TestOuterClass.Item> bidi(Multi<TestOuterClass.Item> request) {
        int contextId = contextChecker.newContextId("TestService#bidi");
        return Multi.createFrom().emitter(
                emitter -> request.subscribe().with(
                        item -> {
                            if (contextChecker.requestContextId() != contextId) {
                                throw new RuntimeException("Different context for subscriber and TestService#bidi method");
                            }
                            Item newItem = new Item();
                            newItem.text = item.getText();
                            dao.add(newItem);
                            emitter.emit(item);
                        }));
    }

}
