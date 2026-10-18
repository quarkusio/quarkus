package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent;

public class ApplyResolveNamesImagePolicyDecorator extends NamedResourceDecorator<PodTemplateSpecFluent<?>> {

    @Override
    public void andThenVisit(PodTemplateSpecFluent<?> podTemplate, ObjectMeta meta) {
        podTemplate.editOrNewMetadata().addToAnnotations("alpha.image.policy.openshift.io/resolve-names", "*").endMetadata();
    }
}
