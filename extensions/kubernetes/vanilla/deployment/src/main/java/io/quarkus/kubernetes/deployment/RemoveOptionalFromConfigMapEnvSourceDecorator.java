package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ConfigMapEnvSourceFluent;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;

public class RemoveOptionalFromConfigMapEnvSourceDecorator extends ApplicationContainerDecorator<ConfigMapEnvSourceFluent> {

    @Override
    public void andThenVisit(ConfigMapEnvSourceFluent ref) {
        ref.withOptional(null);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddEnvVarDecorator.class, ApplicationContainerDecorator.class };
    }
}
