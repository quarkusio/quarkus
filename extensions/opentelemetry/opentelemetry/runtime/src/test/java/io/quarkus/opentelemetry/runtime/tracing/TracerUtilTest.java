package io.quarkus.opentelemetry.runtime.tracing;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class TracerUtilTest {

    @Test
    public void testMapResourceAttributes() {
        List<String> resourceAttributes = Arrays.asList(
                "service.name=myservice",
                "service.namespace=mynamespace",
                "service.version=1.0",
                "deployment.environment=production");
        Resource resource = TracerUtil.mapResourceAttributes(resourceAttributes);
        Attributes attributes = resource.getAttributes();
        Assertions.assertThat(attributes.size()).isEqualTo(4);
        Assertions.assertThat(attributes.get(ResourceAttributes.SERVICE_NAME)).isEqualTo("myservice");
        Assertions.assertThat(attributes.get(ResourceAttributes.SERVICE_NAMESPACE)).isEqualTo("mynamespace");
        Assertions.assertThat(attributes.get(ResourceAttributes.SERVICE_VERSION)).isEqualTo("1.0");
        Assertions.assertThat(attributes.get(ResourceAttributes.DEPLOYMENT_ENVIRONMENT)).isEqualTo("production");
    }
}
