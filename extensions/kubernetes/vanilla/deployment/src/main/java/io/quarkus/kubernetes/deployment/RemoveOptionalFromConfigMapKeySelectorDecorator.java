package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ConfigMapKeySelectorFluent;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;

public class RemoveOptionalFromConfigMapKeySelectorDecorator extends ApplicationContainerDecorator<ConfigMapKeySelectorFluent> {

    @Override
    public void andThenVisit(ConfigMapKeySelectorFluent fluent) {
        fluent.withOptional(null);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddEnvVarDecorator.class, ApplicationContainerDecorator.class };
    }
}
