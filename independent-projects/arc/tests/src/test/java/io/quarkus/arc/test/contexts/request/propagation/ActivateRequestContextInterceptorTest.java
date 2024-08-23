package io.quarkus.arc.test.contexts.request.propagation;

import static io.quarkus.arc.test.contexts.request.propagation.ActivateRequestContextInterceptorTest.FakeSession.State.CLOSED;
import static io.quarkus.arc.test.contexts.request.propagation.ActivateRequestContextInterceptorTest.FakeSession.State.INIT;
import static io.quarkus.arc.test.contexts.request.propagation.ActivateRequestContextInterceptorTest.FakeSession.State.OPENED;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.impl.ActivateRequestContextInterceptor;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.contexts.request.propagation.ActivateRequestContextInterceptorTest.FakeSession.State;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Test the {@link ActivateRequestContextInterceptor} when {@link ActivateRequestContext} is applied to a method.
 */
public class ActivateRequestContextInterceptorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(FakeSessionProducer.class, SessionClient.class,
            ExecutorProducer.class, SessionClientCompletingOnDifferentThread.class);

    @Nested
    class CompletingActivateRequestContext {

        InstanceHandle<SessionClient> clientHandler;

        @BeforeEach
        void reset() {
            FakeSession.state = INIT;
            clientHandler = Arc.container().instance(SessionClient.class);
        }

        @AfterEach
        void terminate() {
            clientHandler.close();
            clientHandler.destroy();
        }

        @Test
        public void testUni() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = clientHandler.get().openUniSession().await().indefinitely();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

        @Test
        public void testMulti() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = clientHandler.get().openMultiSession()
                    .toUni()
                    .await().indefinitely();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

        @Test
        public void testFuture() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = clientHandler.get()
                    .openFutureSession().toCompletableFuture().join();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

        @Test
        public void testStage() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = clientHandler.get().openStageSession()
                    .toCompletableFuture().join();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

        @Test
        public void testNonReactive() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = clientHandler.get().openSession();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

    }

    @Nested
    class AsyncCompletingActivateRequestContext {

        InstanceHandle<SessionClientCompletingOnDifferentThread> asyncClientHandler;

        @BeforeEach
        void reset() {
            FakeSession.state = INIT;
            asyncClientHandler = Arc.container().instance(SessionClientCompletingOnDifferentThread.class);
        }

        @AfterEach
        void terminate() {
            asyncClientHandler.close();
            asyncClientHandler.destroy();
        }

        @Test
        public void testUni() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = asyncClientHandler.get().openUniSession().await().indefinitely();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

        @Test
        public void testMulti() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = asyncClientHandler.get().openMultiSession()
                    .toUni()
                    .await().indefinitely();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

        @Test
        public void testFuture() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = asyncClientHandler.get()
                    .openFutureSession().toCompletableFuture().join();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

        @Test
        public void testStage() throws Exception {
            Assertions.assertEquals(INIT, FakeSession.getState());
            FakeSession.State result = asyncClientHandler.get().openStageSession()
                    .toCompletableFuture().join();

            // Closed in the dispose method
            Assertions.assertEquals(CLOSED, FakeSession.getState());
            Assertions.assertEquals(OPENED, result);
        }

    }

    @ApplicationScoped
    static class SessionClient {
        @Inject
        FakeSession session;

        @ActivateRequestContext
        public Uni<State> openUniSession() {
            return Uni.createFrom()
                    .item(() -> session)
                    .map(FakeSession::open);
        }

        @ActivateRequestContext
        public Multi<State> openMultiSession() {
            return Multi.createFrom()
                    .item(() -> session)
                    .map(FakeSession::open);
        }

        @ActivateRequestContext
        public CompletionStage<State> openStageSession() {
            return CompletableFuture
                    .completedStage(session.open());
        }

        @ActivateRequestContext
        public CompletableFuture<State> openFutureSession() {
            return CompletableFuture
                    .completedFuture(session.open());
        }

        @ActivateRequestContext
        public State openSession() {
            return session.open();
        }
    }

    @ApplicationScoped
    static class SessionClientCompletingOnDifferentThread {
        @Inject
        FakeSession session;

        @Inject
        Executor executor;

        @ActivateRequestContext
        public Uni<State> openUniSession() {
            return Uni.createFrom()
                    .item(() -> session)
                    .map(FakeSession::open)
                    .emitOn(executor);
        }

        @ActivateRequestContext
        public Multi<State> openMultiSession() {
            return Multi.createFrom()
                    .item(() -> session)
                    .map(FakeSession::open)
                    .emitOn(executor);
        }

        @ActivateRequestContext
        public CompletionStage<State> openStageSession() {
            return CompletableFuture.completedStage(session.open())
                    .thenApplyAsync(s -> s, executor);
        }

        @ActivateRequestContext
        public CompletableFuture<State> openFutureSession() {
            return CompletableFuture.completedFuture(session.open())
                    .thenApplyAsync(s -> s, executor);
        }

        @ActivateRequestContext
        public State openSession() {
            return session.open();
        }
    }

    static class FakeSession {
        enum State {
            INIT,
            OPENED,
            CLOSED
        };

        public static State state = State.INIT;

        public State open() {
            state = State.OPENED;
            return state;
        }

        public State close() {
            state = State.CLOSED;
            return state;
        }

        public static State getState() {
            return state;
        }
    }

    @Singleton
    static class FakeSessionProducer {

        @Produces
        @RequestScoped
        FakeSession produceSession() {
            return new FakeSession();
        }

        void disposeSession(@Disposes FakeSession session) {
            session.close();
        }
    }

    @Singleton
    static class ExecutorProducer {

        @Produces
        @ApplicationScoped
        ExecutorService produceExecutor() {
            return Executors.newSingleThreadExecutor();
        }

        void disposeSession(@Disposes ExecutorService executor) {
            executor.shutdown();
        }

    }

}
