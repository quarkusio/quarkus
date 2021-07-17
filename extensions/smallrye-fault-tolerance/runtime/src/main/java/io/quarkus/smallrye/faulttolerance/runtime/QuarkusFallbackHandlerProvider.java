package io.quarkus.smallrye.faulttolerance.runtime;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import io.smallrye.faulttolerance.FallbackHandlerProvider;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Dependent
@Alternative
@Priority(1)
public class QuarkusFallbackHandlerProvider implements FallbackHandlerProvider {

    @Inject
    @Any
    Instance<FallbackHandler<?>> instance;

    @Override
    public <T> FallbackHandler<T> get(FaultToleranceOperation operation) {
        if (operation.hasFallback()) {
            return new FallbackHandler<T>() {
                @SuppressWarnings("unchecked")
                @Override
                public T handle(ExecutionContext context) {
                    Class<? extends FallbackHandler<?>> clazz = operation.getFallback().value();
                    FallbackHandler<T> fallbackHandlerInstance = (FallbackHandler<T>) instance.select(clazz).get();
                    try {
                        return fallbackHandlerInstance.handle(context);
                    } finally {
                        // The instance exists to service a single invocation only
                        instance.destroy(fallbackHandlerInstance);
                    }
                }
            };
        }
        return null;
    }

}
