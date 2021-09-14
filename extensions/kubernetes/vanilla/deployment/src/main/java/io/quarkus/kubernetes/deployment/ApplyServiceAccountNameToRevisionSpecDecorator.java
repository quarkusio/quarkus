
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.knative.serving.v1.RevisionSpecFluent;
import io.fabric8.kubernetes.api.model.ObjectMeta;

public class ApplyServiceAccountNameToRevisionSpecDecorator extends NamedResourceDecorator<RevisionSpecFluent<?>> {
    private static final String NONE = null;
    private final String serviceAccountName;

    public ApplyServiceAccountNameToRevisionSpecDecorator() {
        this(ANY, NONE);
    }

    public ApplyServiceAccountNameToRevisionSpecDecorator(String serviceAccountName) {
        super(ANY);
        this.serviceAccountName = serviceAccountName;
    }

    public ApplyServiceAccountNameToRevisionSpecDecorator(String resourceName, String serviceAccountName) {
        super(resourceName);
        this.serviceAccountName = serviceAccountName;
    }

    public void andThenVisit(RevisionSpecFluent<?> spec, ObjectMeta resourceMeta) {
        if (Strings.isNotNullOrEmpty(this.serviceAccountName)) {
            spec.withServiceAccountName(this.serviceAccountName);
        } else {
            spec.withServiceAccountName(resourceMeta.getName());
        }

    }

    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }
}
