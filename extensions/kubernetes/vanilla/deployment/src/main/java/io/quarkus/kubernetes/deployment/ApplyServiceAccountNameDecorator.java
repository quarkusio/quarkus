package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpecFluent;

public class ApplyServiceAccountNameDecorator extends NamedResourceDecorator<PodSpecFluent> {

    private static final String NONE = null;
    private final String serviceAccountName;

    public ApplyServiceAccountNameDecorator() {
        this(ANY, NONE);
    }

    public ApplyServiceAccountNameDecorator(String serviceAccountName) {
        super(ANY);
        this.serviceAccountName = serviceAccountName;
    }

    public ApplyServiceAccountNameDecorator(String resourceName, String serviceAccountName) {
        super(resourceName);
        this.serviceAccountName = serviceAccountName;
    }

    @Override
    public void andThenVisit(PodSpecFluent podSpec, ObjectMeta resourceMeta) {
        if (Strings.isNotNullOrEmpty(serviceAccountName)) {
            podSpec.withServiceAccountName(serviceAccountName);
        } else {
            podSpec.withServiceAccountName(resourceMeta.getName());
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }

}
