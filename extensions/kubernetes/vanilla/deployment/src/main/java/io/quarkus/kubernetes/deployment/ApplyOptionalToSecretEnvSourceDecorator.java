package io.quarkus.kubernetes.deployment;

import java.util.Set;

import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.model.SecretEnvSourceFluent;

/**
 * Marks the {@code envFrom} Secret references of a container as optional, which the Dekorate
 * environment variable model cannot express.
 */
public class ApplyOptionalToSecretEnvSourceDecorator extends ApplicationContainerDecorator<SecretEnvSourceFluent> {

    private final Set<String> secretNames;

    public ApplyOptionalToSecretEnvSourceDecorator(String containerName, Set<String> secretNames) {
        super((String) null, containerName);
        this.secretNames = secretNames;
    }

    @Override
    public void andThenVisit(SecretEnvSourceFluent ref) {
        if (secretNames.contains(ref.getName())) {
            ref.withOptional(true);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddEnvVarDecorator.class, AddSidecarDecorator.class, AddInitContainerDecorator.class };
    }
}
