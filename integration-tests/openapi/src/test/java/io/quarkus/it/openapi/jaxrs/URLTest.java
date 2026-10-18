package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class URLTest extends AbstractTest {

    // Just URL
    @Test
    public void testJustURLInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testJustURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testJustURLInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justURL", APPLICATION_JSON);
    }

    @Test
    public void testJustURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justURL", APPLICATION_JSON);
    }

    // RestResponse<URL>
    @Test
    public void testRestResponseURLInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponseURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testRestResponseURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponseURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testRestResponseURLInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseURL", APPLICATION_JSON);
    }

    @Test
    public void testRestResponseURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseURL", APPLICATION_JSON);
    }

    // Optional<URL>
    // @Test
    public void testOptionalURLInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    // @Test
    public void testOptionalURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testOptionalURLInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalURL", APPLICATION_JSON);
    }

    @Test
    public void testOptionalURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalURL", APPLICATION_JSON);
    }

    // Uni<URL>
    @Test
    public void testUniURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testUniURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniURL", APPLICATION_JSON);
    }

    // CompletionStage<URL>
    @Test
    public void testCompletionStageURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStageURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testCompletionStageURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageURL", APPLICATION_JSON);
    }

    // CompletedFuture<URL>
    @Test
    public void testCompletedFutureURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFutureURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testCompletedFutureURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completedFutureURL", APPLICATION_JSON);
    }

    // List<URL>
    @Test
    public void testListURLInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listURL", APPLICATION_JSON, "[\"https://quarkus.io/\"]");
    }

    @Test
    public void testListURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listURL", APPLICATION_JSON, "[\"https://quarkus.io/\"]");
    }

    @Test
    public void testListURLInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listURL", APPLICATION_JSON);
    }

    @Test
    public void testListURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listURL", APPLICATION_JSON);
    }

    // URL[]
    @Test
    public void testArrayURLInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayURL", APPLICATION_JSON, "[\"https://quarkus.io/\"]");
    }

    @Test
    public void testArrayURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayURL", APPLICATION_JSON, "[\"https://quarkus.io/\"]");
    }

    @Test
    public void testArrayURLInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayURL", APPLICATION_JSON);
    }

    @Test
    public void testArrayURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayURL", APPLICATION_JSON);
    }

    // Map<URL,URL>
    @Test
    public void testMapURLInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/mapURL", APPLICATION_JSON, "{\"mapURL\":\"https://quarkus.io/\"}");
    }

    @Test
    public void testMapURLInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/mapURL", APPLICATION_JSON, "{\"mapURL\":\"https://quarkus.io/\"}");
    }

    @Test
    public void testMapURLInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/mapURL", APPLICATION_JSON);
    }

    @Test
    public void testMapURLInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/mapURL", APPLICATION_JSON);
    }
}
