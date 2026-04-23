package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that a receiver method runs in its own request context, isolated from the caller's request context.
 * The {@link RequestScoped} bean injected in the receiver must be a different instance
 * from the one obtained in the caller's request context.
 */
public class RequestScopeIsolationTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(
                    root -> root.addClasses(MyReceivers.class, SignalSender.class, IdentityService.class, BlockingCmd.class,
                            ReactiveCmd.class));

    @Inject
    SignalSender sender;

    @Test
    public void testBlockingReceiverHasSeparateRequestScope() {
        sender.sendBlocking()
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
    }

    @Test
    public void testReactiveReceiverHasSeparateRequestScope() {
        sender.sendReactive()
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
    }

    @Test
    public void testBlockingReceiverHasSeparateRequestScopeAwait() {
        sender.sendBlockingAndAwait();
    }

    @Test
    public void testReactiveReceiverHasSeparateRequestScopeAwait() {
        sender.sendReactiveAndAwait();
    }

    @Singleton
    public static class SignalSender {

        @Inject
        Signal<BlockingCmd> blockingSignal;

        @Inject
        Signal<ReactiveCmd> reactiveSignal;

        @Inject
        IdentityService identityService;

        @ActivateRequestContext
        Uni<Void> sendBlocking() {
            int outerIdentity = identityService.getId();
            return blockingSignal.reactive().request(new BlockingCmd(), Integer.class)
                    .onItem().invoke(receiverIdentity -> {
                        assertEquals(outerIdentity, identityService.getId());
                        assertNotEquals(outerIdentity, receiverIdentity.intValue(),
                                "Blocking receiver should run in a separate request scope");
                    })
                    .replaceWithVoid();
        }

        @ActivateRequestContext
        Uni<Void> sendReactive() {
            int outerIdentity = identityService.getId();
            return reactiveSignal.reactive().request(new ReactiveCmd(), Integer.class)
                    .onItem().invoke(receiverIdentity -> {
                        assertEquals(outerIdentity, identityService.getId());
                        assertNotEquals(outerIdentity, receiverIdentity.intValue(),
                                "Reactive receiver should run in a separate request scope");
                    })
                    .replaceWithVoid();
        }

        @ActivateRequestContext
        void sendBlockingAndAwait() {
            int outerIdentity = identityService.getId();
            int receiverIdentity = blockingSignal.request(new BlockingCmd(), Integer.class);
            assertEquals(outerIdentity, identityService.getId());
            assertNotEquals(outerIdentity, receiverIdentity,
                    "Blocking receiver should run in a separate request scope");
        }

        @ActivateRequestContext
        void sendReactiveAndAwait() {
            int outerIdentity = identityService.getId();
            int receiverIdentity = reactiveSignal.request(new ReactiveCmd(), Integer.class);
            assertEquals(outerIdentity, identityService.getId());
            assertNotEquals(outerIdentity, receiverIdentity,
                    "Reactive receiver should run in a separate request scope");
        }
    }

    @RequestScoped
    public static class IdentityService {

        private int id;

        @PostConstruct
        void init() {
            id = ThreadLocalRandom.current().nextInt();
        }

        public int getId() {
            return id;
        }
    }

    @Singleton
    public static class MyReceivers {

        // Blocking signature (returns int) → BLOCKING
        int blocking(@Receives BlockingCmd cmd, IdentityService identityService) {
            return identityService.getId();
        }

        // Uni return type → NON_BLOCKING
        Uni<Integer> reactive(@Receives ReactiveCmd cmd, IdentityService identityService) {
            return Uni.createFrom().item(identityService.getId());
        }
    }

    record BlockingCmd() {
    }

    record ReactiveCmd() {
    }
}
