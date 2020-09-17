package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ContainerBuilder;
import io.dekorate.kubernetes.config.Env;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;

/**
 * Delegate to dekorate's version to sidestep the fact that the dekorate version of AddEnvVarDecorator inherits from
 * ApplicationContainerDecorator which expects metadata to be present, which doesn't happen in our context. We sidestep the
 * issue by wrapping the original behavior and calling its andThenVisit method, which does the work when not skipped.
 */
public class AddEnvVarDecorator extends Decorator<ContainerBuilder> {
    private final io.dekorate.kubernetes.decorator.AddEnvVarDecorator delegate;

    public AddEnvVarDecorator(Env env) {
        delegate = new io.dekorate.kubernetes.decorator.AddEnvVarDecorator(env);
    }

    @Override
    public void visit(ContainerBuilder containerBuilder) {
        delegate.andThenVisit(containerBuilder);
    }

    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class };
    }
}
