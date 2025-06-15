package io.quarkus.kubernetes.deployment;

import io.dekorate.knative.decorator.AddConfigMapVolumeToRevisionDecorator;
import io.dekorate.knative.decorator.AddSecretVolumeToRevisionDecorator;
import io.dekorate.kubernetes.decorator.AddConfigMapVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddSecretVolumeDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelectorFluent;

public class RemoveOptionalFromConfigMapKeySelectorDecorator
        extends ApplicationContainerDecorator<ConfigMapKeySelectorFluent> {

    @Override
    public void andThenVisit(ConfigMapKeySelectorFluent fluent) {
        fluent.withOptional(null);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddEnvVarDecorator.class, AddSecretVolumeDecorator.class,
                AddSecretVolumeToRevisionDecorator.class, AddConfigMapVolumeToRevisionDecorator.class,
                AddConfigMapVolumeDecorator.class };
    }
}
