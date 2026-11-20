package io.quarkus.opentelemetry.runtime.tracing;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAMESPACE;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes.SERVICE_INSTANCE_ID;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;

public class TracerUtilTest {

    @Test
    public void testMapResourceAttributes() {
        List<String> resourceAttributes = Arrays.asList(
                "service.name=myservice",
                "service.namespace=mynamespace",
                "service.version=1.0",
                "deployment.environment.name=production");
        Resource resource = TracerUtil.mapResourceAttributes(resourceAttributes, null);
        Attributes attributes = resource.getAttributes();
        Assertions.assertThat(attributes.size()).isEqualTo(3);
        Assertions.assertThat(attributes.get(SERVICE_NAME)).isNull();
        Assertions.assertThat(attributes.get(SERVICE_NAMESPACE)).isEqualTo("mynamespace");
        Assertions.assertThat(attributes.get(SERVICE_VERSION)).isEqualTo("1.0");
        Assertions.assertThat(attributes.get(DEPLOYMENT_ENVIRONMENT_NAME)).isEqualTo("production");
    }

    @Test
    public void testEntityOwnedAttributesAreNotMapped() {
        List<String> resourceAttributes = Arrays.asList(
                "service.name=myservice",
                "service.instance.id=42",
                "service.version=1.0");
        Resource resource = TracerUtil.mapResourceAttributes(resourceAttributes, null);
        Attributes attributes = resource.getAttributes();
        // the SDK already applied these two and built resource entities from them, writing them again
        // here would drop the entity association
        Assertions.assertThat(attributes.size()).isEqualTo(1);
        Assertions.assertThat(attributes.get(SERVICE_NAME)).isNull();
        Assertions.assertThat(attributes.get(SERVICE_INSTANCE_ID)).isNull();
        Assertions.assertThat(attributes.get(SERVICE_VERSION)).isEqualTo("1.0");
    }
}
