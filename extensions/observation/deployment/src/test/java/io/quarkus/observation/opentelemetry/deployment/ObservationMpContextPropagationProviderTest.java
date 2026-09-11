package io.quarkus.observation.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.junit.jupiter.api.Test;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.quarkus.observation.propagation.ObservationContextStorage;
import io.quarkus.observation.propagation.ObservationMpContextPropagationProvider;

/**
 * Plain unit test (no Quarkus boot) for {@link ObservationMpContextPropagationProvider}.
 * <p>
 * Focuses on the "cleared" context contract: when MicroProfile Context Propagation is configured to
 * clear the observation context (e.g. {@code ManagedExecutor} with {@code ThreadContext.ALL_REMAINING}
 * cleared), the task must run with no ambient observation scope, and the previous scope must be
 * restored afterwards. Because managed threads are pooled, a scope left on the target thread would
 * otherwise leak into a task that explicitly asked for a cleared context.
 */
public class ObservationMpContextPropagationProviderTest {

    private static ObservationRegistry registryWithHandler() {
        ObservationRegistry registry = ObservationRegistry.create();
        // A registry with no handlers is a no-op and would hand out no-op scopes; add a handler so
        // openScope() actually sets the current scope on the thread.
        registry.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        });
        return registry;
    }

    @Test
    void clearedContextClearsAmbientScopeAndRestoresItAfterwards() throws Exception {
        ObservationMpContextPropagationProvider provider = new ObservationMpContextPropagationProvider();
        ObservationRegistry registry = registryWithHandler();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Simulate a pooled managed thread that already holds an observation scope (left over from
            // a previous task). The scope is opened directly on the worker thread and never closed.
            executor.submit(() -> {
                Observation leaked = Observation.createNotStarted("leaked", registry);
                leaked.start();
                leaked.openScope();
                assertThat(ObservationContextStorage.currentScope())
                        .as("precondition: the worker thread holds a leftover scope")
                        .isNotNull();
            }).get(5, TimeUnit.SECONDS);

            // A task submitted with the observation context CLEARED must not see the leftover scope.
            ThreadContextSnapshot snapshot = provider.clearedContext(Map.of());
            boolean clearedInsideTask = executor.submit(() -> {
                ThreadContextController controller = snapshot.begin();
                try {
                    return ObservationContextStorage.currentScope() == null;
                } finally {
                    controller.endContext();
                }
            }).get(5, TimeUnit.SECONDS);

            assertThat(clearedInsideTask)
                    .as("clearedContext must clear any ambient observation scope on the target thread")
                    .isTrue();

            // After the task ends, the previously present scope must be restored on the worker thread.
            boolean restoredAfterTask = executor.submit(
                    () -> ObservationContextStorage.currentScope() != null)
                    .get(5, TimeUnit.SECONDS);

            assertThat(restoredAfterTask)
                    .as("clearedContext must restore the previous scope once the task completes")
                    .isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
