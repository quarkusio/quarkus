package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceFluent;

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
