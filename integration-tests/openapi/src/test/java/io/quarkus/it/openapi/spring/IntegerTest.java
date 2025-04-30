package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class IntegerTest extends AbstractTest {

    // Just Integer
    @Test
    public void testJustIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justInteger", TEXT_PLAIN);
    }

    @Test
    public void testJustIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justInteger", TEXT_PLAIN);
    }

    // Just int
    @Test
    public void testJustIntInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justInt", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustIntInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justInt", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustIntInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justInt", TEXT_PLAIN);
    }

    @Test
    public void testJustIntInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justInt", TEXT_PLAIN);
    }

    // ResponseEntity<Integer>
    @Test
    public void testResponseEntityIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/responseEntityInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testResponseEntityIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/responseEntityInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testResponseEntityIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityInteger", TEXT_PLAIN);
    }

    @Test
    public void testResponseEntityIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityInteger", TEXT_PLAIN);
    }

    // Optional<Integer>
    //@Test
    public void testOptionalIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalInteger", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalInteger", TEXT_PLAIN);
    }

    @Test
    public void testOptionalIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalInteger", TEXT_PLAIN);
    }

    // OptionalInt
    //@Test
    public void testOptionalIntInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalInt", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalIntInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalInt", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalIntInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalInt", TEXT_PLAIN);
    }

    @Test
    public void testOptionalIntInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalInt", TEXT_PLAIN);
    }

    // Uni<Integer>
    @Test
    public void testUniIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniInteger", TEXT_PLAIN);
    }

    // CompletionStage<Integer>
    @Test
    public void testCompletionStageIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStageInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageInteger", TEXT_PLAIN);
    }

    // CompletedFuture<Integer>
    @Test
    public void testCompletedFutureIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFutureInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageInteger", TEXT_PLAIN);
    }

    // List<Integer>
    @Test
    public void testListIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listInteger", APPLICATION_JSON);
    }

    @Test
    public void testListIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listInteger", APPLICATION_JSON);
    }

    // Integer[]
    @Test
    public void testArrayIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayInteger", APPLICATION_JSON);
    }

    @Test
    public void testArrayIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayInteger", APPLICATION_JSON);
    }

    // int[]
    @Test
    public void testArrayIntInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayInt", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayIntInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayInt", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayIntInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayInt", APPLICATION_JSON);
    }

    @Test
    public void testArrayIntInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayInt", APPLICATION_JSON);
    }

    // Map<Integer, Integer>
    @Test
    public void testMapIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/mapInteger", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/mapInteger", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/mapInteger", APPLICATION_JSON);
    }

    @Test
    public void testMapIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/mapInteger", APPLICATION_JSON);
    }
}
