package io.quarkus.opentelemetry.runtime;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.incubating.WebengineIncubatingAttributes.WEBENGINE_NAME;
import static io.opentelemetry.semconv.incubating.WebengineIncubatingAttributes.WEBENGINE_VERSION;

import java.util.function.Predicate;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the {@link DelayedAttributes} bean, from the Quarkus version and the service version.
 */
public class DelayedAttributesCreator implements BeanCreator<DelayedAttributes> {

    public static final String QUARKUS_VERSION_PARAM = "quarkusVersion";
    public static final String SERVICE_VERSION_PARAM = "serviceVersion";

    @Override
    public DelayedAttributes create(SyntheticCreationalContext<DelayedAttributes> context) {
        OpenTelemetryUtil.checkNotStaticInit(DelayedAttributes.class);
        var result = new DelayedAttributes();
        // These are merged into the resource after the SDK built it, so they must not carry
        // an attribute the SDK built a resource entity from. Resource.getDefault() carries
        // service.name="unknown_service:java", which would drop the service entity, and the
        // SDK already starts its own resource from Resource.getDefault() anyway.
        AttributesBuilder attributes = Resource.getDefault().getAttributes().toBuilder();
        attributes.removeIf(new Predicate<AttributeKey<?>>() {
            @Override
            public boolean test(AttributeKey<?> key) {
                return ResourceConfigReconciler.ENTITY_OWNED_ATTRIBUTES.contains(key.getKey());
            }
        });
        attributes.put(SERVICE_VERSION, (String) context.getParams().get(SERVICE_VERSION_PARAM));
        attributes.put(WEBENGINE_NAME, "Quarkus");
        attributes.put(WEBENGINE_VERSION, (String) context.getParams().get(QUARKUS_VERSION_PARAM));
        result.setAttributesDelegate(attributes.build());
        return result;
    }
}
