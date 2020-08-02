
package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.SecretEnvSourceFluent;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;

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
