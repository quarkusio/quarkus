package io.quarkus.opentelemetry.runtime.tracing;

import static io.opentelemetry.semconv.DeploymentAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAMESPACE;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;

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
    public void testServiceNameIsNotMapped() {
        List<String> resourceAttributes = Arrays.asList(
                "service.name=myservice",
                "service.version=1.0");
        Resource resource = TracerUtil.mapResourceAttributes(resourceAttributes, null);
        Attributes attributes = resource.getAttributes();
        // the SDK always builds the service entity from otel.service.name, so writing the attribute
        // again here would drop the entity association
        Assertions.assertThat(attributes.size()).isEqualTo(1);
        Assertions.assertThat(attributes.get(SERVICE_NAME)).isNull();
        Assertions.assertThat(attributes.get(SERVICE_VERSION)).isEqualTo("1.0");
    }

    @Test
    public void testServiceInstanceIdIsMapped() {
        List<String> resourceAttributes = Arrays.asList(
                "service.instance.id=42",
                "service.version=1.0");
        Resource resource = TracerUtil.mapResourceAttributes(resourceAttributes, null);
        Attributes attributes = resource.getAttributes();
        // A user supplied id switches the SDK's service-instance-id provider off, so no
        // service.instance entity exists to collide with. Writing it here is also the only thing that
        // delivers the value when the attributes were given in the indexed list form, which reaches
        // the SDK as otel.resource.attributes[0] and is never read.
        Assertions.assertThat(attributes.size()).isEqualTo(2);
        Assertions.assertThat(attributes.get(SERVICE_INSTANCE_ID)).isEqualTo("42");
        Assertions.assertThat(attributes.get(SERVICE_VERSION)).isEqualTo("1.0");
    }
}
