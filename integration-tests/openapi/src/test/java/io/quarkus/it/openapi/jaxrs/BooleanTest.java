package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BooleanTest extends AbstractTest {

    // Just Boolean
    @Test
    public void testJustBooleanInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testJustBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testJustBooleanInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justBoolean", TEXT_PLAIN);
    }

    @Test
    public void testJustBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justBoolean", TEXT_PLAIN);
    }

    // Just boolean
    @Test
    public void testJustBoolInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justBool", TEXT_PLAIN, "true");
    }

    @Test
    public void testJustBoolInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justBool", TEXT_PLAIN, "true");
    }

    @Test
    public void testJustBoolInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justBool", TEXT_PLAIN);
    }

    @Test
    public void testJustBoolInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justBool", TEXT_PLAIN);
    }

    // RestResponse<Boolean>
    @Test
    public void testRestResponseBooleanInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponseBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testRestResponseBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponseBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testRestResponseBooleanInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseBoolean", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseBoolean", TEXT_PLAIN);
    }

    // Optional<Boolean>
    //@Test
    public void testOptionalBooleanInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalBoolean", TEXT_PLAIN, "true");
    }

    //@Test
    public void testOptionalBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testOptionalBooleanInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalBoolean", TEXT_PLAIN);
    }

    @Test
    public void testOptionalBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalBoolean", TEXT_PLAIN);
    }

    // Uni<Boolean>

    @Test
    public void testUniBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testUniBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniBoolean", TEXT_PLAIN);
    }

    // CompletionStage<Boolean>

    @Test
    public void testCompletionStageBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStageBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testCompletionStageBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageBoolean", TEXT_PLAIN);
    }

    // CompletedFuture<Boolean>
    @Test
    public void testCompletedFutureBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFutureBoolean", TEXT_PLAIN, "true");
    }

    @Test
    public void testCompletedFutureBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageBoolean", TEXT_PLAIN);
    }

    // List<Boolean>
    @Test
    public void testListBooleanInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listBoolean", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testListBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listBoolean", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testListBooleanInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listBoolean", APPLICATION_JSON);
    }

    @Test
    public void testListBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listBoolean", APPLICATION_JSON);
    }

    // Boolean[]
    @Test
    public void testArrayBooleanInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayBoolean", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testArrayBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayBoolean", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testArrayBooleanInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayBoolean", APPLICATION_JSON);
    }

    @Test
    public void testArrayBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayBoolean", APPLICATION_JSON);
    }

    // boolean[]
    @Test
    public void testArrayBoolInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayBool", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testArrayBoolInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayBool", APPLICATION_JSON, "[true]");
    }

    @Test
    public void testArrayBoolInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayBool", APPLICATION_JSON);
    }

    @Test
    public void testArrayBoolInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayBool", APPLICATION_JSON);
    }

    // Map<Boolean, Boolean>
    @Test
    public void testMapBooleanInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/mapBoolean", APPLICATION_JSON, "{\"true\":true}");
    }

    @Test
    public void testMapBooleanInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/mapBoolean", APPLICATION_JSON, "{\"true\":true}");
    }

    @Test
    public void testMapBooleanInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/mapBoolean", APPLICATION_JSON);
    }

    @Test
    public void testMapBooleanInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/mapBoolean", APPLICATION_JSON);
    }
}
