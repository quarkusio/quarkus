package io.quarkus.opentelemetry.runtime.tracing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TracerRecorder {

    public static final Set<String> dropNonApplicationUriTargets = new HashSet<>();
    public static final Set<String> dropStaticResourceTargets = new HashSet<>();

    /* STATIC INIT */
    public void setAttributes(
            BeanContainer beanContainer,
            String quarkusVersion,
            String serviceName,
            String serviceVersion) {

        DelayedAttributes delayedAttributes = beanContainer.beanInstance(DelayedAttributes.class);

        delayedAttributes.setAttributesDelegate(Resource.getDefault()
                .merge(Resource.create(
                        Attributes.of(
                                ResourceAttributes.SERVICE_NAME, serviceName,
                                ResourceAttributes.SERVICE_VERSION, serviceVersion,
                                ResourceAttributes.WEBENGINE_NAME, "Quarkus",
                                ResourceAttributes.WEBENGINE_VERSION, quarkusVersion)))
                .getAttributes());
    }

    /* STATIC INIT */
    public void setupSampler(
            List<String> dropNonApplicationUris,
            List<String> dropStaticResources) {
        dropNonApplicationUriTargets.addAll(dropNonApplicationUris);
        dropStaticResourceTargets.addAll(dropStaticResources);
    }
}
