
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.model.SecretEnvSourceFluent;

public class RemoveOptionalFromSecretEnvSourceDecorator extends ApplicationContainerDecorator<SecretEnvSourceFluent> {

    @Override
    public void andThenVisit(SecretEnvSourceFluent ref) {
        ref.withOptional(null);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddEnvVarDecorator.class, ApplicationContainerDecorator.class };
    }

}
