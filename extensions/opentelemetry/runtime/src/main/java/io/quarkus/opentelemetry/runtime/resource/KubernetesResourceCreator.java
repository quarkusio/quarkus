package io.quarkus.opentelemetry.runtime.resource;

import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;

/**
 * Creates the {@link Resource} bean with the Kubernetes attributes of the running application.
 */
public class KubernetesResourceCreator implements BeanCreator<Resource> {

    @Override
    public Resource create(SyntheticCreationalContext<Resource> context) {
        OpenTelemetryUtil.checkNotStaticInit(Resource.class);
        return KubernetesResourceProvider.createResource();
    }
}
