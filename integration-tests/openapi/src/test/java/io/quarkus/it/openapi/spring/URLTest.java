package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class URLTest extends AbstractTest {

    // Just URL
    @Test
    public void testJustURLInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testJustURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testJustURLInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justURL", APPLICATION_JSON);
    }

    @Test
    public void testJustURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justURL", APPLICATION_JSON);
    }

    // RestResponse<URL>
    @Test
    public void testRestResponseURLInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/restResponseURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testRestResponseURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/restResponseURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testRestResponseURLInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/restResponseURL", APPLICATION_JSON);
    }

    @Test
    public void testRestResponseURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/restResponseURL", APPLICATION_JSON);
    }

    // Optional<URL>
    // @Test
    public void testOptionalURLInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    // @Test
    public void testOptionalURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testOptionalURLInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalURL", APPLICATION_JSON);
    }

    @Test
    public void testOptionalURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalURL", APPLICATION_JSON);
    }

    // Uni<URL>
    @Test
    public void testUniURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testUniURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniURL", APPLICATION_JSON);
    }

    // CompletionStage<URL>
    @Test
    public void testCompletionStageURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStageURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testCompletionStageURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageURL", APPLICATION_JSON);
    }

    // CompletedFuture<URL>
    @Test
    public void testCompletedFutureURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFutureURL", APPLICATION_JSON, "\"https://quarkus.io/\"");
    }

    @Test
    public void testCompletedFutureURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completedFutureURL", APPLICATION_JSON);
    }

    // List<URL>
    @Test
    public void testListURLInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listURL", APPLICATION_JSON, "[\"https://quarkus.io/\"]");
    }

    @Test
    public void testListURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listURL", APPLICATION_JSON, "[\"https://quarkus.io/\"]");
    }

    @Test
    public void testListURLInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listURL", APPLICATION_JSON);
    }

    @Test
    public void testListURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listURL", APPLICATION_JSON);
    }

    // URL[]
    @Test
    public void testArrayURLInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayURL", APPLICATION_JSON, "[\"https://quarkus.io/\"]");
    }

    @Test
    public void testArrayURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayURL", APPLICATION_JSON, "[\"https://quarkus.io/\"]");
    }

    @Test
    public void testArrayURLInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayURL", APPLICATION_JSON);
    }

    @Test
    public void testArrayURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayURL", APPLICATION_JSON);
    }

    // Map<URL,URL>
    @Test
    public void testMapURLInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/mapURL", APPLICATION_JSON, "{\"mapURL\":\"https://quarkus.io/\"}");
    }

    @Test
    public void testMapURLInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/mapURL", APPLICATION_JSON, "{\"mapURL\":\"https://quarkus.io/\"}");
    }

    @Test
    public void testMapURLInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/mapURL", APPLICATION_JSON);
    }

    @Test
    public void testMapURLInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/mapURL", APPLICATION_JSON);
    }
}
