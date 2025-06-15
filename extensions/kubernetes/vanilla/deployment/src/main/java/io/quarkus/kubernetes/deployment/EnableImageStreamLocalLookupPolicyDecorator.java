package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.ImageStreamSpecFluent;

public class EnableImageStreamLocalLookupPolicyDecorator extends NamedResourceDecorator<ImageStreamSpecFluent<?>> {

    public EnableImageStreamLocalLookupPolicyDecorator() {
        super("ImageStream", ANY);
    }

    public EnableImageStreamLocalLookupPolicyDecorator(String name) {
        super("ImageStream", name);
    }

    @Override
    public void andThenVisit(ImageStreamSpecFluent<?> spec, ObjectMeta meta) {
        spec.withNewLookupPolicy().withLocal().endLookupPolicy();
    }
}
