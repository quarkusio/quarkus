package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpecFluent;

public class ApplyServiceAccountNameDecorator extends NamedResourceDecorator<PodSpecFluent<?>> {

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

    public String getServiceAccountNameOrDefault(String defaultName) {
        return Strings.isNotNullOrEmpty(serviceAccountName) ? serviceAccountName : defaultName;
    }

    @Override
    public void andThenVisit(PodSpecFluent podSpec, ObjectMeta resourceMeta) {
        podSpec.withServiceAccountName(getServiceAccountNameOrDefault(resourceMeta.getName()));
    }

    @Override
    public Class<? extends Decorator<?>>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }

}
