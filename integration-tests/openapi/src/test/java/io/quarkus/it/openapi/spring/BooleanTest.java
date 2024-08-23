package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BooleanTest extends AbstractTest {

    // Just Boolean
    @Test
    public void testJustBooleanInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testJustBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testJustBooleanInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justBoolean", TEXT_PLAIN);
    }

    @Test
    public void testJustBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justBoolean", TEXT_PLAIN);
    }

    // Just boolean
    @Test
    public void testJustBoolInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justBool", TEXT_PLAIN, "true");
    }

    @Test
    public void testJustBoolInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justBool", TEXT_PLAIN, "true");
    }

    @Test
    public void testJustBoolInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justBool", TEXT_PLAIN);
    }

    @Test
    public void testJustBoolInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justBool", TEXT_PLAIN);
    }

    // ResponseEntity<Boolean>
    @Test
    public void testResponseEntityBooleanInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/responseEntityBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testResponseEntityBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/responseEntityBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testResponseEntityBooleanInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityBoolean", TEXT_PLAIN);
    }

    @Test
    public void testResponseEntityBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityBoolean", TEXT_PLAIN);
    }

    // Optional<Boolean>
    //@Test
    public void testOptionalBooleanInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalBoolean", TEXT_PLAIN, "true");
    }

    //@Test
    public void testOptionalBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testOptionalBooleanInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalBoolean", TEXT_PLAIN);
    }

    @Test
    public void testOptionalBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalBoolean", TEXT_PLAIN);
    }

    // Uni<Boolean>
    @Test
    public void testUniBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testUniBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniBoolean", TEXT_PLAIN);
    }

    // CompletionStage<Boolean>
    @Test
    public void testCompletionStageBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStageBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testCompletionStageBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageBoolean", TEXT_PLAIN);
    }

    // CompletedFuture<Boolean>
    @Test
    public void testCompletedFutureBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFutureBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testCompletedFutureBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageBoolean", TEXT_PLAIN);
    }

    // List<Boolean>
    @Test
    public void testListBooleanInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listBoolean", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testListBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listBoolean", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testListBooleanInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listBoolean", APPLICATION_JSON);
    }

    @Test
    public void testListBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listBoolean", APPLICATION_JSON);
    }

    // Boolean[]
    @Test
    public void testArrayBooleanInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayBoolean", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testArrayBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayBoolean", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testArrayBooleanInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayBoolean", APPLICATION_JSON);
    }

    @Test
    public void testArrayBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayBoolean", APPLICATION_JSON);
    }

    // boolean[]
    @Test
    public void testArrayBoolInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayBool", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testArrayBoolInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayBool", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testArrayBoolInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayBool", APPLICATION_JSON);
    }

    @Test
    public void testArrayBoolInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayBool", APPLICATION_JSON);
    }

    // Map<Boolean, Boolean>
    @Test
    public void testMapBooleanInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/mapBoolean", APPLICATION_JSON, "{\"true\":true}");
    }

    @Test
    public void testMapBooleanInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/mapBoolean", APPLICATION_JSON, "{\"true\":true}");
    }

    @Test
    public void testMapBooleanInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/mapBoolean", APPLICATION_JSON);
    }

    @Test
    public void testMapBooleanInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/mapBoolean", APPLICATION_JSON);
    }
}
