package io.quarkus.observation.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.quarkus.observation.propagation.QuarkusObservationRegistry;

/**
 * Plain unit test (no Quarkus boot) for the {@code QuarkusObservationRegistry} contract.
 *
 * <p>
 * Off any Vert.x context, the registry uses Micrometer's static ThreadLocal as its store, so these
 * assertions exercise the ThreadLocal fallback path and the delegation to the stock registry.
 */
public class QuarkusObservationRegistryTest {

    private final QuarkusObservationRegistry registry = new QuarkusObservationRegistry();

    @AfterEach
    void clearThreadLocal() {
        // The underlying store is a JVM-wide static ThreadLocal; make sure we don't leak scopes.
        registry.setCurrentObservationScope(null);
    }

    @Test
    void isNoopAndConfigAreDelegated() {
        assertThat(registry.observationConfig()).isNotNull();
        // A registry with no handlers is a no-op; registering a handler must flip isNoop(),
        // proving both isNoop() and observationConfig() are delegated to the backing registry.
        assertThat(registry.isNoop()).isTrue();
        registry.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        });
        assertThat(registry.isNoop()).isFalse();
    }

    @Test
    void noCurrentScopeReturnsNull() {
        assertThat(registry.getCurrentObservationScope()).isNull();
        assertThat(registry.getCurrentObservation()).isNull();
    }

    @Test
    void setAndGetCurrentScopeRoundTrips() {
        Observation.Scope scope = new NoopScope();

        registry.setCurrentObservationScope(scope);
        assertThat(registry.getCurrentObservationScope()).isSameAs(scope);

        registry.setCurrentObservationScope(null);
        assertThat(registry.getCurrentObservationScope()).isNull();
    }

    @SuppressWarnings("deprecation")
    private static final class NoopScope implements Observation.Scope {

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
