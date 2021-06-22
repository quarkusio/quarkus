package com.example.grpc.hibernate;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;

import com.example.test.TestOuterClass;
import com.example.test.TestRawGrpc;

import io.grpc.stub.StreamObserver;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

@GrpcService
@Blocking
public class RawTestService extends TestRawGrpc.TestRawImplBase {

    private static final Logger log = Logger.getLogger(RawTestService.class);
    private static final TestOuterClass.Empty EMPTY = TestOuterClass.Empty.getDefaultInstance();

    @Inject
    EntityManager entityManager;

    @Inject
    ItemDao dao;

    @Inject
    ContextChecker contextChecker;

    ManagedContext requestContext;

    @PostConstruct
    public void setUp() {
        requestContext = Arc.container().requestContext();
    }

    @Override
    @Transactional
    public void add(TestOuterClass.Item request, StreamObserver<TestOuterClass.Empty> responseObserver) {
        contextChecker.newContextId("RawTestService#add");
        Item item = new Item();
        item.text = request.getText();
        entityManager.persist(item);
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    @Transactional
    public void clear(TestOuterClass.Empty request, StreamObserver<TestOuterClass.Empty> responseObserver) {
        contextChecker.newContextId("RawTestService#clear");
        entityManager.createQuery("DELETE from Item")
                .executeUpdate();
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    public void getAll(TestOuterClass.Empty request, StreamObserver<TestOuterClass.Item> responseObserver) {
        contextChecker.newContextId("RawTestService#getAll");
        List<Item> items = entityManager.createQuery("from Item", Item.class)
                .getResultList();
        for (Item item : items) {
            responseObserver.onNext(TestOuterClass.Item.newBuilder().setText(item.text).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<TestOuterClass.Item> bidi(StreamObserver<TestOuterClass.Item> responseObserver) {
        int contextId = contextChecker.newContextId("RawTestService#bidi");

        return new StreamObserver<TestOuterClass.Item>() {
            @Override
            public void onNext(TestOuterClass.Item value) {
                if (contextChecker.requestContextId() != contextId) {
                    throw new RuntimeException("Different context for onNext and RawTestService#bidi method");
                }
                Item newItem = new Item();
                newItem.text = value.getText();
                dao.add(newItem);

                responseObserver.onNext(value);
            }

            @Override
            public void onError(Throwable t) {
                log.error("bidi onError", t);
            }

            @Override
            public void onCompleted() {
                if (contextChecker.requestContextId() != contextId) {
                    throw new RuntimeException("Different context for onCompleted and RawTestService#bidi method");
                }
                if (!requestContext.isActive()) {
                    throw new RuntimeException("Request context not active for `onCompleted`");
                }
                responseObserver.onCompleted();
            }
        };
    }

}
