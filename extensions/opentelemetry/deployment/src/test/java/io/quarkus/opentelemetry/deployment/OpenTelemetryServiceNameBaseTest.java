package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.internal.EntityUtil;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.ServiceAttributes;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.restassured.RestAssured;

public abstract class OpenTelemetryServiceNameBaseTest {

    protected static final String SERVICE_NAME = "FrankBullitt";

    @Inject
    TestSpanExporter spanExporter;

    @Test
    void testServiceName() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /hello", server.getName());
        assertEquals(SERVICE_NAME, server.getResource().getAttribute(AttributeKey.stringKey("service.name")));
        assertServiceEntity(server.getResource());
    }

    /**
     * The attribute value alone is not enough: since SDK 1.67 a resource also carries resource
     * entities and writing service.name as an ordinary
     * attribute keeps the value but silently drops the service entity. Entity introspection is
     * internal API, which is acceptable in a test but never in runtime code.
     */
    private static void assertServiceEntity(Resource resource) {
        assertThat(EntityUtil.getEntities(resource))
                .as("the service resource entity must survive, not only the service.name value")
                .filteredOn(entity -> "service".equals(entity.getType()))
                .singleElement()
                .extracting(entity -> entity.getId().get(ServiceAttributes.SERVICE_NAME))
                .isEqualTo(SERVICE_NAME);
    }

    @Path("/hello")
    public static class HelloResource {
        @GET
        public String hello() {
            return "hello";
        }
    }
}
