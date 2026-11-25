package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.internal.EntityUtil;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.ServiceAttributes;
import io.quarkus.opentelemetry.deployment.common.ResourceWarningGuard;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Resource attributes written in the indexed list form, which SmallRye Config accepts alongside the
 * comma separated one. They are forwarded as {@code otel.resource.attributes[0]} and the SDK only
 * ever reads the exact key {@code otel.resource.attributes}, so none of them reach it: every one has
 * to be delivered by Quarkus from the parsed list.
 * <p>
 * The lone {@code service.instance.id} at index 0 is the originally reported case; the rest cover a
 * list with several entries, including an ordinary attribute and the two the SDK builds resource
 * entities from.
 */
public class OpenTelemetryIndexedResourceAttributesTest {

    private static final String SERVICE_NAME = "cart";
    private static final String INSTANCE_ID = "42";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = ResourceWarningGuard.apply(new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestSpanExporterProvider.class)
                    .addClass(ResourceWarningGuard.class))
            .overrideConfigKey("quarkus.application.name", "application-name-must-fail")
            .overrideRuntimeConfigKey("quarkus.otel.bsp.schedule.delay", "50")// speed up test
            .overrideRuntimeConfigKey("quarkus.otel.resource.attributes[0]", "service.instance.id=" + INSTANCE_ID)
            .overrideRuntimeConfigKey("quarkus.otel.resource.attributes[1]", "service.name=" + SERVICE_NAME)
            .overrideRuntimeConfigKey("quarkus.otel.resource.attributes[2]", "deployment.environment.name=test")
            .overrideRuntimeConfigKey("quarkus.otel.resource.attributes[3]", "service.namespace=shopping")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false"));

    @Inject
    TestSpanExporter spanExporter;

    @Test
    void everyIndexedAttributeReachesTheResource() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        Resource resource = getSpanByKindAndParentId(spans, SERVER, "0000000000000000").getResource();

        assertThat(attribute(resource, "service.instance.id"))
                .as("an entity owned attribute is delivered by Quarkus, since the SDK never sees it")
                .isEqualTo(INSTANCE_ID);
        assertThat(attribute(resource, "service.name"))
                .as("the service name is resolved from the indexed form too")
                .isEqualTo(SERVICE_NAME);
        assertThat(attribute(resource, "deployment.environment.name"))
                .as("an ordinary attribute alongside the entity owned ones")
                .isEqualTo("test");
        assertThat(attribute(resource, "service.namespace")).isEqualTo("shopping");

        assertThat(EntityUtil.getEntities(resource))
                .as("the service resource entity must be built from the indexed service.name too")
                .filteredOn(entity -> "service".equals(entity.getType()))
                .singleElement()
                .extracting(entity -> entity.getId().get(ServiceAttributes.SERVICE_NAME))
                .isEqualTo(SERVICE_NAME);

        assertThat(EntityUtil.getEntities(resource))
                .as("a user supplied service.instance.id leaves no service.instance entity")
                .noneMatch(entity -> "service.instance".equals(entity.getType()));
    }

    private static String attribute(Resource resource, String key) {
        return resource.getAttribute(AttributeKey.stringKey(key));
    }

    @Path("/hello")
    public static class HelloResource {
        @GET
        public String hello() {
            return "hello";
        }
    }
}
