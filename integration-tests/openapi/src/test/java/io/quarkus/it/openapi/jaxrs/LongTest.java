package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LongTest extends AbstractTest {

    // Just Long
    @Test
    public void testJustLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justLong", TEXT_PLAIN);
    }

    @Test
    public void testJustLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justLong", TEXT_PLAIN);
    }

    // Just long
    @Test
    public void testJustPrimitiveLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justPrimitiveLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustPrimitiveLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justPrimitiveLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustPrimitiveLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justPrimitiveLong", TEXT_PLAIN);
    }

    @Test
    public void testJustPrimitiveLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justPrimitiveLong", TEXT_PLAIN);
    }

    // RestResponse<Long>
    @Test
    public void testRestResponseLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponseLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponseLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseLong", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseLong", TEXT_PLAIN);
    }

    // Optional<Long>
    //@Test
    public void testOptionalLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalLong", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalLong", TEXT_PLAIN);
    }

    @Test
    public void testOptionalLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalLong", TEXT_PLAIN);
    }

    // OptionalLong
    //@Test
    public void testOptionalPrimitiveLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalPrimitiveLong", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalPrimitiveLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalPrimitiveLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalPrimitiveLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalPrimitiveLong", TEXT_PLAIN);
    }

    @Test
    public void testOptionalPrimitiveLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalPrimitiveLong", TEXT_PLAIN);
    }

    // Uni<Long>
    @Test
    public void testUniLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniLong", TEXT_PLAIN);
    }

    // CompletionStage<Long>
    @Test
    public void testCompletionStageLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStageLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageLong", TEXT_PLAIN);
    }

    // CompletedFuture<Long>
    @Test
    public void testCompletedFutureLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFutureLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageLong", TEXT_PLAIN);
    }

    // List<Long>
    @Test
    public void testListLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listLong", APPLICATION_JSON);
    }

    @Test
    public void testListLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listLong", APPLICATION_JSON);
    }

    // Long[]
    @Test
    public void testArrayLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayLong", APPLICATION_JSON);
    }

    @Test
    public void testArrayLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayLong", APPLICATION_JSON);
    }

    // long[]
    @Test
    public void testArrayPrimitiveLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayPrimitiveLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayPrimitiveLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayPrimitiveLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayPrimitiveLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayPrimitiveLong", APPLICATION_JSON);
    }

    @Test
    public void testArrayPrimitiveLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayPrimitiveLong", APPLICATION_JSON);
    }

    // Map<Long, Long>
    @Test
    public void testMapLongInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/mapLong", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapLongInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/mapLong", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapLongInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/mapLong", APPLICATION_JSON);
    }

    @Test
    public void testMapLongInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/mapLong", APPLICATION_JSON);
    }
}
