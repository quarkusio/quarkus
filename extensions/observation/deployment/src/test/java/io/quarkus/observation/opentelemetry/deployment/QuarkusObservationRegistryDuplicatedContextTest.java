package io.quarkus.observation.opentelemetry.deployment;

import static io.quarkus.observation.propagation.ObservationContextStorage.OBSERVATION_SCOPE_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.quarkus.observation.propagation.ObservationContextStorage;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Verifies that the real {@link ObservationRegistry} bean (a
 * {@code QuarkusObservationRegistry}) makes the Vert.x duplicated context the
 * <em>primary</em> scope store.
 *
 * <p>
 * Unlike most observation tests, this one deliberately does NOT register a
 * {@code TestObservationRegistry} alternative, so {@code @Inject ObservationRegistry}
 * resolves to the actual Quarkus registry and every scope get/set goes through
 * {@code ObservationContextStorage}.
 */
public class QuarkusObservationRegistryDuplicatedContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            "quarkus.log.category.\"io.quarkus.observation.propagation.ObservationContextStorage\".level=DEBUG\n"),
                            "application.properties"));

    @Inject
    Vertx vertx;

    @Inject
    ObservationRegistry registry;

    /**
     * On a duplicated context, a scope opened through the real registry must be
     * stored in the duplicated context, NOT in Micrometer's static ThreadLocal.
     */
    @Test
    void scopeIsStoredInDuplicatedContextNotThreadLocal() throws Exception {
        final Context duplicatedContext = VertxContext.getOrCreateDuplicatedContext(vertx);

        final Boolean threadLocalUntouched = runOnContext(duplicatedContext, () -> {
            final Observation observation = Observation.createNotStarted("duplicatedContext-primary", registry);
            observation.start();
            final Observation.Scope scope = observation.openScope();
            try {
                // The real registry resolves the current observation from the duplicated context.
                assertThat(registry.getCurrentObservation()).isSameAs(observation);

                // Look directly into the duplicated context's local storage: the scope must be
                // stored there under the storage key.
                assertThat(VertxContext.localContextData(duplicatedContext).get(OBSERVATION_SCOPE_KEY))
                        .as("scope must be stored in the duplicated context's local data")
                        .isSameAs(scope);

                // A fresh SimpleObservationRegistry reads Micrometer's JVM-wide static ThreadLocal.
                // It must be empty.
                return ObservationRegistry.create().getCurrentObservationScope() == null;
            } finally {
                scope.close();
                observation.stop();
            }
        });

        assertThat(threadLocalUntouched)
                .as("scope opened on a duplicated context must not be written to the static ThreadLocal")
                .isTrue();

        // After the scope is closed, the duplicated context's local data must no longer hold it.
        Boolean cleared = runOnContext(duplicatedContext,
                () -> !VertxContext.localContextData(duplicatedContext).containsKey(OBSERVATION_SCOPE_KEY));
        assertThat(cleared)
                .as("closing the scope must remove it from the duplicated context's local data")
                .isTrue();
    }

    /**
     * A scope opened on a duplicated context must remain visible on a subsequent
     * dispatch on the same duplicated context (the whole point of DC storage over
     * a plain ThreadLocal).
     */
    @Test
    void scopeSurvivesReDispatchOnSameDuplicatedContext() throws Exception {
        Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);

        // First dispatch: open a scope and leave it open in the duplicated context.
        Observation observation = runOnContext(dc, () -> {
            Observation obs = Observation.createNotStarted("dc-redispatch", registry);
            obs.start();
            obs.openScope();
            return obs;
        });

        // Second, independent dispatch on the same duplicated context.
        Boolean visible = runOnContext(dc, () -> registry.getCurrentObservation() == observation);
        assertThat(visible)
                .as("scope must still be current on a later dispatch on the same duplicated context")
                .isTrue();

        // Clean up the scope we intentionally left open.
        runOnContext(dc, () -> {
            Observation.Scope current = registry.getCurrentObservationScope();
            if (current != null) {
                current.close();
            }
            observation.stop();
            return null;
        });
    }

    /**
     * A scope opened on one duplicated context must not be visible on a different
     * duplicated context — scopes must not leak between concurrent requests.
     */
    @Test
    void scopesAreIsolatedBetweenDuplicatedContexts() throws Exception {
        Context dc1 = VertxContext.getOrCreateDuplicatedContext(vertx);
        Context dc2 = VertxContext.getOrCreateDuplicatedContext(vertx);

        Observation observation = runOnContext(dc1, () -> {
            Observation obs = Observation.createNotStarted("dc-isolation", registry);
            obs.start();
            obs.openScope();
            return obs;
        });

        try {
            Observation leaked = runOnContext(dc2, () -> registry.getCurrentObservation());
            assertThat(leaked)
                    .as("a scope opened on dc1 must not be visible on dc2")
                    .isNull();
        } finally {
            runOnContext(dc1, () -> {
                Observation.Scope current = registry.getCurrentObservationScope();
                if (current != null) {
                    current.close();
                }
                observation.stop();
                return null;
            });
        }
    }

    private <T> T runOnContext(Context context, ContextAction<T> action) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        context.runOnContext(v -> {
            try {
                future.complete(action.run());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future.get(5, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface ContextAction<T> {
        T run() throws Exception;
    }
}
