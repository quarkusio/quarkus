package io.quarkus.kubernetes.deployment;

import java.util.Set;

import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceFluent;

/**
 * Marks the {@code envFrom} ConfigMap references of a container as optional, which the Dekorate
 * environment variable model cannot express.
 */
public class ApplyOptionalToConfigMapEnvSourceDecorator extends ApplicationContainerDecorator<ConfigMapEnvSourceFluent> {

    private final Set<String> configMapNames;

    public ApplyOptionalToConfigMapEnvSourceDecorator(String containerName, Set<String> configMapNames) {
        super((String) null, containerName);
        this.configMapNames = configMapNames;
    }

    @Override
    public void andThenVisit(ConfigMapEnvSourceFluent ref) {
        if (configMapNames.contains(ref.getName())) {
            ref.withOptional(true);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddEnvVarDecorator.class, AddSidecarDecorator.class, AddInitContainerDecorator.class };
    }
}
