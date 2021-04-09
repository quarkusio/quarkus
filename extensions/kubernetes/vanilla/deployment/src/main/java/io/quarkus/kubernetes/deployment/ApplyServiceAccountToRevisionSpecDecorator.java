
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.knative.serving.v1.RevisionSpecFluent;
import io.fabric8.kubernetes.api.model.ObjectMeta;

public class ApplyServiceAccountToRevisionSpecDecorator extends NamedResourceDecorator<RevisionSpecFluent<?>> {
    private static final String NONE = null;
    private final String serviceAccount;

    public ApplyServiceAccountToRevisionSpecDecorator() {
        this(ANY, NONE);
    }

    public ApplyServiceAccountToRevisionSpecDecorator(String serviceAccount) {
        super(ANY);
        this.serviceAccount = serviceAccount;
    }

    public ApplyServiceAccountToRevisionSpecDecorator(String resourceName, String serviceAccount) {
        super(resourceName);
        this.serviceAccount = serviceAccount;
    }

    public void andThenVisit(RevisionSpecFluent<?> spec, ObjectMeta resourceMeta) {
        if (Strings.isNotNullOrEmpty(this.serviceAccount)) {
            spec.withServiceAccount(this.serviceAccount);
        } else {
            spec.withServiceAccount(resourceMeta.getName());
        }

    }

    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }
}
