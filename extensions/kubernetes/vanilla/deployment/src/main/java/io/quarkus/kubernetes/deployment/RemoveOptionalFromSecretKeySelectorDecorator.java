
package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.SecretKeySelectorFluent;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;

public class RemoveOptionalFromSecretKeySelectorDecorator extends ApplicationContainerDecorator<SecretKeySelectorFluent> {

    @Override
    public void andThenVisit(SecretKeySelectorFluent fluent) {
        fluent.withOptional(null);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddEnvVarDecorator.class, ApplicationContainerDecorator.class };
    }
}
