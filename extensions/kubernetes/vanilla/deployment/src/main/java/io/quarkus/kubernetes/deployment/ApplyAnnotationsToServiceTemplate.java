package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.utils.Maps;
import io.fabric8.knative.serving.v1.ServiceFluent;
import io.fabric8.kubernetes.api.model.ObjectMeta;

/**
 * This class was created to workaround https://github.com/dekorateio/dekorate/issues/869.
 * Once this issue is fixed, we can delete this and use the provided by Dekorate.
 */
public class ApplyAnnotationsToServiceTemplate extends NamedResourceDecorator<ServiceFluent<?>> {

    private static final String SERVICE_KIND = "Service";

    private final Map<String, String> annotations;

    public ApplyAnnotationsToServiceTemplate(String name, String... annotations) {
        this(name, Maps.from(annotations));
    }

    public ApplyAnnotationsToServiceTemplate(String name, Map<String, String> annotations) {
        super(SERVICE_KIND, name);
        this.annotations = annotations;
    }

    @Override
    public void andThenVisit(ServiceFluent<?> service, ObjectMeta resourceMeta) {
        service.editOrNewSpec().editOrNewTemplate().editOrNewMetadata()
                .addToAnnotations(annotations)
                .endMetadata().endTemplate().endSpec();
    }
}
