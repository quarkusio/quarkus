package io.quarkus.opentelemetry.runtime.tracing;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.incubating.WebengineIncubatingAttributes.WEBENGINE_NAME;
import static io.opentelemetry.semconv.incubating.WebengineIncubatingAttributes.WEBENGINE_VERSION;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.StaticInit;

@Recorder
public class TracerRecorder {

    public static final Set<String> dropNonApplicationUriTargets = new HashSet<>();
    public static final Set<String> dropStaticResourceTargets = new HashSet<>();

    @StaticInit
    public Supplier<DelayedAttributes> delayedAttributes(String quarkusVersion,
            String serviceName,
            String serviceVersion) {
        return new Supplier<>() {
            @Override
            public DelayedAttributes get() {
                var result = new DelayedAttributes();
                result.setAttributesDelegate(Resource.getDefault()
                        .merge(Resource.create(
                                Attributes.of(
                                        SERVICE_NAME, serviceName,
                                        SERVICE_VERSION, serviceVersion,
                                        WEBENGINE_NAME, "Quarkus",
                                        WEBENGINE_VERSION, quarkusVersion)))
                        .getAttributes());
                return result;
            }
        };
    }

    @StaticInit
    public void setupSampler(
            List<String> dropNonApplicationUris,
            List<String> dropStaticResources) {
        dropNonApplicationUriTargets.addAll(dropNonApplicationUris);
        dropStaticResourceTargets.addAll(dropStaticResources);
    }

}
