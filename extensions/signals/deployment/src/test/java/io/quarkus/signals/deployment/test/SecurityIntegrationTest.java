package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.spi.ReceiverInterceptor;
import io.quarkus.signals.spi.SignalMetadataEnricher;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

public class SecurityIntegrationTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    Cmd.class, MyReceivers.class, SecurityIntegration.class, IdentityMock.class));

    @Inject
    Signal<Cmd> signal;

    @ActivateRequestContext
    @Test
    public void testIntegration() {
        MyReceivers.CMDS.clear();

        IdentityMock.setUpAuth(IdentityMock.ADMIN);
        String result = signal.reactive().request(new Cmd("Hello"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("hello", result);

        assertThrows(UnauthorizedException.class, () -> {
            IdentityMock.setUpAuth(IdentityMock.ANONYMOUS);
            signal.reactive().request(new Cmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });
        assertEquals(1, MyReceivers.CMDS.size());
        assertEquals("Hello", MyReceivers.CMDS.get(0).value());
    }

    // --- Signal types ---

    record Cmd(String value) {
    }

    // --- Receivers ---

    @RolesAllowed("admin")
    @Singleton
    public static class MyReceivers {

        static final List<Cmd> CMDS = new CopyOnWriteArrayList<>();

        String process(@Receives Cmd cmd) {
            CMDS.add(cmd);
            return cmd.value().toLowerCase();
        }

    }

    // --- Enrichers, interceptors ---

    @Identifier("quarkus.security")
    @Singleton
    public static class SecurityIntegration implements SignalMetadataEnricher, ReceiverInterceptor {

        @Inject
        CurrentIdentityAssociation currentIdentity;

        @Override
        public void enrich(EnrichmentContext context) {
            context.putMetadata("identity", currentIdentity.getDeferredIdentity());
        }

        @SuppressWarnings("unchecked")
        @Override
        public Uni<Object> intercept(InterceptionContext context) {
            currentIdentity.setIdentity((Uni<SecurityIdentity>) context.signalContext().metadata().get("identity"));
            return context.proceed();
        }
    }

}
