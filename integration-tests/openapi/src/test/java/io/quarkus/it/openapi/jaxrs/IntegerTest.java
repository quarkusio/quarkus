package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class IntegerTest extends AbstractTest {

    // Just Integer
    @Test
    public void testJustIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justInteger", TEXT_PLAIN);
    }

    @Test
    public void testJustIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justInteger", TEXT_PLAIN);
    }

    // Just int
    @Test
    public void testJustIntInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justInt", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustIntInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justInt", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustIntInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justInt", TEXT_PLAIN);
    }

    @Test
    public void testJustIntInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justInt", TEXT_PLAIN);
    }

    // RestResponse<Integer>
    @Test
    public void testRestResponseIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponseInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponseInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseInteger", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseInteger", TEXT_PLAIN);
    }

    // Optional<Integer>
    //@Test
    public void testOptionalIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalInteger", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalInteger", TEXT_PLAIN);
    }

    @Test
    public void testOptionalIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalInteger", TEXT_PLAIN);
    }

    // OptionalInt
    //@Test
    public void testOptionalIntInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalInt", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalIntInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalInt", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalIntInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalInt", TEXT_PLAIN);
    }

    @Test
    public void testOptionalIntInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalInt", TEXT_PLAIN);
    }

    // Uni<Integer>
    @Test
    public void testUniIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniInteger", TEXT_PLAIN);
    }

    // CompletionStage<Integer>
    @Test
    public void testCompletionStageIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStageInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageInteger", TEXT_PLAIN);
    }

    // CompletedFuture<Integer>
    @Test
    public void testCompletedFutureIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFutureInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageInteger", TEXT_PLAIN);
    }

    // List<Integer>
    @Test
    public void testListIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listInteger", APPLICATION_JSON);
    }

    @Test
    public void testListIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listInteger", APPLICATION_JSON);
    }

    // Integer[]
    @Test
    public void testArrayIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayInteger", APPLICATION_JSON);
    }

    @Test
    public void testArrayIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayInteger", APPLICATION_JSON);
    }

    // int[]
    @Test
    public void testArrayIntInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayInt", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayIntInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayInt", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayIntInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayInt", APPLICATION_JSON);
    }

    @Test
    public void testArrayIntInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayInt", APPLICATION_JSON);
    }

    // Map<Integer, Integer>
    @Test
    public void testMapIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/mapInteger", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/mapInteger", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/mapInteger", APPLICATION_JSON);
    }

    @Test
    public void testMapIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/mapInteger", APPLICATION_JSON);
    }
}
