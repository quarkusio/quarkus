package io.quarkus.observation.opentelemetry.deployment;

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
 * Reproduces the condition reported by Clement in
 * https://github.com/quarkusio/quarkus/pull/55145#discussion_r3932645403
 *
 * <p>
 * When {@link ObservationContextStorage} runs on a plain (non-duplicated) Vert.x
 * context, its {@code getVertxContext()} helper creates a brand-new duplicated
 * context on every invocation without ever dispatching on it. As a consequence:
 * <ul>
 * <li>{@code setCurrentScope()} stores the scope in an ephemeral duplicated
 * context that is immediately discarded, and</li>
 * <li>the following {@code currentScope()} creates yet another new (empty)
 * duplicated context,</li>
 * </ul>
 * so the scope is silently lost between the two calls even though both run on
 * the very same underlying Vert.x context.
 */
public class ObservationContextStorageTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            "quarkus.log.category.\"io.quarkus.observation.propagation.ObservationContextStorage\".level=DEBUG\n"),
                            "application.properties"));

    @Inject
    Vertx vertx;

    /**
     * The reproducer: on a non-duplicated Vert.x context, a scope set with
     * {@link ObservationContextStorage#setCurrentScope(Observation.Scope)} must
     * be visible to a subsequent
     * {@link ObservationContextStorage#currentScope()} on the same context.
     *
     * <p>
     * This was failing at some point.
     */
    @Test
    void scopeSetOnNonDuplicatedContextMustBeRetrievable() throws Exception {
        // A root event-loop context created from a non-Vert.x thread is NOT duplicated.
        Context nonDuplicatedContext = vertx.getOrCreateContext();
        Observation.Scope scope = new RecordingScope();

        Result result = runOnContext(nonDuplicatedContext, () -> {
            boolean onDuplicated = VertxContext.isOnDuplicatedContext();
            ObservationContextStorage.setCurrentScope(scope);
            Observation.Scope retrieved = ObservationContextStorage.currentScope();
            return new Result(onDuplicated, retrieved);
        });

        // Guard: make sure we genuinely exercised the non-duplicated-context branch.
        assertThat(result.onDuplicatedContext())
                .as("the reproducer must run on a non-duplicated Vert.x context")
                .isFalse();

        assertThat(result.retrieved())
                .as("scope stored on a non-duplicated context must be retrievable on the same context")
                .isSameAs(scope);
    }

    /**
     * Control test: on a duplicated context the round-trip already works today.
     * This confirms the test harness itself is correct and isolates the failure
     * above to the non-duplicated-context branch.
     */
    @Test
    void scopeSetOnDuplicatedContextIsRetrievable() throws Exception {
        Context duplicatedContext = VertxContext.getOrCreateDuplicatedContext(vertx);
        Observation.Scope scope = new RecordingScope();

        Result result = runOnContext(duplicatedContext, () -> {
            boolean onDuplicated = VertxContext.isOnDuplicatedContext();
            ObservationContextStorage.setCurrentScope(scope);
            Observation.Scope retrieved = ObservationContextStorage.currentScope();
            return new Result(onDuplicated, retrieved);
        });

        assertThat(result.onDuplicatedContext())
                .as("this control must run on a duplicated Vert.x context")
                .isTrue();

        assertThat(result.retrieved())
                .as("scope stored on a duplicated context must be retrievable on the same context")
                .isSameAs(scope);
    }

    /**
     * On a duplicated context, {@link ObservationContextStorage#setCurrentScope(Observation.Scope)}
     * must return the previously stored scope so callers (e.g. {@code SimpleScope.close()}) can
     * restore it, and nested set/restore must round-trip correctly.
     */
    @Test
    void setCurrentScopeReturnsPreviousAndStacksOnDuplicatedContext() throws Exception {
        Context duplicatedContext = VertxContext.getOrCreateDuplicatedContext(vertx);
        Observation.Scope a = new RecordingScope();
        Observation.Scope b = new RecordingScope();

        StackResult result = runOnContext(duplicatedContext, () -> {
            Observation.Scope prev0 = ObservationContextStorage.setCurrentScope(a);
            Observation.Scope afterA = ObservationContextStorage.currentScope();
            Observation.Scope prevA = ObservationContextStorage.setCurrentScope(b);
            Observation.Scope afterB = ObservationContextStorage.currentScope();
            // Simulate closing b -> restore a.
            Observation.Scope prevB = ObservationContextStorage.setCurrentScope(a);
            Observation.Scope restored = ObservationContextStorage.currentScope();
            // Simulate closing a -> restore nothing.
            ObservationContextStorage.setCurrentScope(null);
            Observation.Scope afterClear = ObservationContextStorage.currentScope();
            return new StackResult(prev0, afterA, prevA, afterB, prevB, restored, afterClear);
        });

        assertThat(result.prev0()).as("no scope is set initially").isNull();
        assertThat(result.afterA()).isSameAs(a);
        assertThat(result.prevA()).as("setting b must return the previous scope a").isSameAs(a);
        assertThat(result.afterB()).isSameAs(b);
        assertThat(result.prevB()).as("restoring a must return the previous scope b").isSameAs(b);
        assertThat(result.restored()).isSameAs(a);
        assertThat(result.afterClear()).as("clearing must leave no current scope").isNull();
    }

    /**
     * Clearing the scope on a duplicated context must remove the entry from the duplicated
     * context local data rather than leaving a stale {@code null} mapping behind.
     */
    @Test
    void clearingScopeRemovesTheDuplicatedContextEntry() throws Exception {
        Context duplicatedContext = VertxContext.getOrCreateDuplicatedContext(vertx);
        Observation.Scope scope = new RecordingScope();
        String key = ObservationContextStorage.class.getName() + ".observationScope";

        Boolean removed = runOnContext(duplicatedContext, () -> {
            ObservationContextStorage.setCurrentScope(scope);
            boolean presentAfterSet = VertxContext.localContextData(duplicatedContext).containsKey(key);
            ObservationContextStorage.setCurrentScope(null);
            boolean presentAfterClear = VertxContext.localContextData(duplicatedContext).containsKey(key);
            return presentAfterSet && !presentAfterClear;
        });

        assertThat(removed)
                .as("scope key must be present after set and removed (not left as a null entry) after clear")
                .isTrue();
    }

    /**
     * When on a duplicated context but nothing has been stored there, {@code currentScope()} must
     * fall through to Micrometer's static ThreadLocal. This bridges scopes opened through a foreign
     * registry (e.g. a {@code TestObservationRegistry}) that does not delegate to this storage.
     */
    @Test
    void duplicatedContextEmptyFallsThroughToThreadLocal() throws Exception {
        Context duplicatedContext = VertxContext.getOrCreateDuplicatedContext(vertx);

        Boolean sameScopeDifferentRegistries = runOnContext(duplicatedContext, () -> {
            // A SimpleObservationRegistry stores its scope in Micrometer's static ThreadLocal,
            // bypassing ObservationContextStorage. Nothing is put in the duplicated context.
            ObservationRegistry foreign = ObservationRegistry.create();
            Observation observation = Observation.start("foreign", foreign);
            Observation.Scope threadLocalScope = observation.openScope();
            try {
                return ObservationContextStorage.currentScope() == threadLocalScope;
            } finally {
                threadLocalScope.close();
                observation.stop();
            }
        });

        assertThat(sameScopeDifferentRegistries)
                .as("on a duplicated context with nothing stored, currentScope() must fall back to the ThreadLocal")
                .isTrue();
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

    private record Result(boolean onDuplicatedContext, Observation.Scope retrieved) {
    }

    private record StackResult(Observation.Scope prev0, Observation.Scope afterA, Observation.Scope prevA,
            Observation.Scope afterB, Observation.Scope prevB, Observation.Scope restored,
            Observation.Scope afterClear) {
    }

    /**
     * Minimal {@link Observation.Scope} test double used only for identity checks.
     */
    @SuppressWarnings("deprecation")
    private static final class RecordingScope implements Observation.Scope {

        @Override
        public Observation getCurrentObservation() {
            return null;
        }

        @Override
        public void makeCurrent() {
            // no-op
        }

        @Override
        public void reset() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }
    }
}
