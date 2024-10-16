package io.quarkus.smallrye.faulttolerance.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import io.smallrye.faulttolerance.BeforeRetryHandlerProvider;
import io.smallrye.faulttolerance.api.BeforeRetryHandler;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Dependent
@Alternative
@Priority(1)
public class QuarkusBeforeRetryHandlerProvider implements BeforeRetryHandlerProvider {

    @Inject
    @Any
    Instance<BeforeRetryHandler> instance;

    @Override
    public BeforeRetryHandler get(FaultToleranceOperation operation) {
        if (operation.hasBeforeRetry()) {
            return new BeforeRetryHandler() {
                @Override
                public void handle(ExecutionContext context) {
                    Class<? extends BeforeRetryHandler> clazz = operation.getBeforeRetry().value();
                    BeforeRetryHandler handler = instance.select(clazz).get();
                    try {
                        handler.handle(context);
                    } finally {
                        // The instance exists to service a single invocation only
                        instance.destroy(handler);
                    }
                }
            };
        }
        return null;
    }

}
