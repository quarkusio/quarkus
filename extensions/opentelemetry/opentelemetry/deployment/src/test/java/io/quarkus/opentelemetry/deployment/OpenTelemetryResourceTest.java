package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.extension.resources.OsResource;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetryResourceTest {
    private static final String RESOURCE_ATTRIBUTES = "quarkus.opentelemetry.tracer.resource-attributes";

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.opentelemetry.tracer.resources", "os")
            .setBeforeAllCustomizer(() -> System.setProperty(RESOURCE_ATTRIBUTES, "service.name=authservice"))
            .setAfterAllCustomizer(() -> System.getProperties().remove(RESOURCE_ATTRIBUTES))
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestUtil.class)
                    .addAsResource("resource-config/application.properties"));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Resource resource = TestUtil.getResource(openTelemetry);

        assertEquals("authservice", resource.getAttributes().get(ResourceAttributes.SERVICE_NAME));
        assertThat(resource.getAttributes().get(ResourceAttributes.OS_TYPE), not(emptyOrNullString()));
    }

    @ApplicationScoped
    public static class OtelConfiguration {

        @Produces
        public Resource resource() {
            return OsResource.get();
        }
    }
}
