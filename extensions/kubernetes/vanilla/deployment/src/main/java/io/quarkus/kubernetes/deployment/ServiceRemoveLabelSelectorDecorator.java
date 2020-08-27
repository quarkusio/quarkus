package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.kubernetes.api.model.ServiceSpecFluent;

public class ServiceRemoveLabelSelectorDecorator extends RemoveLabelSelectorDecorator<ServiceSpecFluent> {

    public ServiceRemoveLabelSelectorDecorator(String label) {
        super(label);
    }

    @Override
    public void andThenVisit(ServiceSpecFluent spec, ObjectMeta objectMeta) {
        spec.removeFromSelector(label);
    }
}
