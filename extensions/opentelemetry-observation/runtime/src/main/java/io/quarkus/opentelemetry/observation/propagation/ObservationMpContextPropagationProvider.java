package io.quarkus.opentelemetry.observation.propagation;

import static io.smallrye.common.vertx.VertxContext.isDuplicatedContext;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.opentelemetry.observation.handler.AbstractTracingObservationHandler;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.smallrye.common.vertx.VertxContext;

public class ObservationMpContextPropagationProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Observation observation = getCurrentObservation();

        // Use anonymous classes instead of lambdas for the native image
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                ObservationRegistry registry = getRegistry();
                if (registry == null) {
                    return new ThreadContextController() {
                        @Override
                        public void endContext() throws IllegalStateException {
                        }
                    };
                }

                Observation.Scope previousScope = registry.getCurrentObservationScope();
                if (observation != null) {
                    registry.setCurrentObservationScope(
                            new PropagatedObservationScope(observation, previousScope));
                    return new ThreadContextController() {
                        @Override
                        public void endContext() throws IllegalStateException {
                            registry.setCurrentObservationScope(previousScope);
                        }
                    };
                }
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                    }
                };
            }
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                    }
                };
            }
        };
    }

    @Override
    public String getThreadContextType() {
        return "MicrometerObservation";
    }

    private Observation getCurrentObservation() {
        io.vertx.core.Context vertxCtx = QuarkusContextStorage.getVertxContext();
        if (vertxCtx != null && isDuplicatedContext(vertxCtx)) {
            return (Observation) VertxContext.localContextData(vertxCtx)
                    .get(AbstractTracingObservationHandler.OBSERVATION_KEY);
        }
        return null;
    }

    private ObservationRegistry getRegistry() {
        ArcContainer container = Arc.container();
        if (container == null) {
            return null;
        }
        InstanceHandle<ObservationRegistry> handle = container.instance(ObservationRegistry.class);
        if (handle.isAvailable()) {
            return handle.get();
        }
        return null;
    }
}
