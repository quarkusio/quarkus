package io.quarkus.arc.test.contexts.request.propagation;

import static io.quarkus.arc.test.contexts.request.propagation.ActivateRequestContextInterceptorTest.FakeSession.State.CLOSED;
import static io.quarkus.arc.test.contexts.request.propagation.ActivateRequestContextInterceptorTest.FakeSession.State.INIT;
import static io.quarkus.arc.test.contexts.request.propagation.ActivateRequestContextInterceptorTest.FakeSession.State.OPENED;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.impl.ActivateRequestContextInterceptor;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.contexts.request.propagation.ActivateRequestContextInterceptorTest.FakeSession.State;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test the {@link ActivateRequestContextInterceptor} when {@link ActivateRequestContext} is applied to a method.
 */
public class ActivateRequestContextInterceptorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(FakeSessionProducer.class, SessionClient.class);

    InstanceHandle<SessionClient> clientHandler;

    @BeforeEach
    public void reset() {
        FakeSession.state = INIT;
        clientHandler = Arc.container().instance(SessionClient.class);
    }

    @AfterEach
    public void terminate() {
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

}
