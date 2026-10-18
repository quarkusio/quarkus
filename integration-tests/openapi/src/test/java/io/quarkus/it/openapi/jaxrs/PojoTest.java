package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PojoTest extends AbstractTest {

    // Just Pojo
    @Test
    public void testJustPojoInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justPojo", APPLICATION_JSON, createExpected("justPojo"));
    }

    @Test
    public void testJustPojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justPojo", APPLICATION_JSON, createExpected("justPojo"));
    }

    @Test
    public void testJustPojoInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justPojo", APPLICATION_JSON);
    }

    @Test
    public void testJustPojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justPojo", APPLICATION_JSON);
    }

    // RestResponse<Pojo>
    @Test
    public void testRestResponsePojoInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponsePojo", APPLICATION_JSON, createExpected("restResponsePojo"));
    }

    @Test
    public void testRestResponsePojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponsePojo", APPLICATION_JSON,
                createExpected("restResponsePojo"));
    }

    @Test
    public void testRestResponsePojoInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponsePojo", APPLICATION_JSON);
    }

    @Test
    public void testRestResponsePojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponsePojo", APPLICATION_JSON);
    }

    // Optional<Pojo>
    @Test
    public void testOptionalPojoInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalPojo", APPLICATION_JSON, createExpected("optionalPojo"));
    }

    @Test
    public void testOptionalPojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalPojo", APPLICATION_JSON, createExpected("optionalPojo"));
    }

    @Test
    public void testOptionalPojoInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalPojo", APPLICATION_JSON);
    }

    @Test
    public void testOptionalPojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalPojo", APPLICATION_JSON);
    }

    // Uni<Pojo>
    @Test
    public void testUniPojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniPojo", APPLICATION_JSON, createExpected("uniPojo"));
    }

    @Test
    public void testUniPojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniPojo", APPLICATION_JSON);
    }

    // CompletionStage<Pojo>
    @Test
    public void testCompletionStagePojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStagePojo", APPLICATION_JSON,
                createExpected("completionStagePojo"));
    }

    @Test
    public void testCompletionStagePojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStagePojo", APPLICATION_JSON);
    }

    // CompletedFuture<Pojo>
    @Test
    public void testCompletedFuturePojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFuturePojo", APPLICATION_JSON,
                createExpected("completedFuturePojo"));
    }

    @Test
    public void testCompletedFuturePojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completedFuturePojo", APPLICATION_JSON);
    }

    // List<Pojo>
    @Test
    public void testListPojoInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listPojo", APPLICATION_JSON, createExpectedList("listPojo"));
    }

    @Test
    public void testListPojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listPojo", APPLICATION_JSON, createExpectedList("listPojo"));
    }

    @Test
    public void testListPojoInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listPojo", APPLICATION_JSON);
    }

    @Test
    public void testListPojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listPojo", APPLICATION_JSON);
    }

    // Pojo[]
    @Test
    public void testArrayPojoInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayPojo", APPLICATION_JSON, createExpectedList("arrayPojo"));
    }

    @Test
    public void testArrayPojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayPojo", APPLICATION_JSON, createExpectedList("arrayPojo"));
    }

    @Test
    public void testArrayPojoInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayPojo", APPLICATION_JSON);
    }

    @Test
    public void testArrayPojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayPojo", APPLICATION_JSON);
    }

    // Map<String, Greeting>
    @Test
    public void testMapPojoInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/mapPojo", APPLICATION_JSON, createExpectedMap("mapPojo"));
    }

    @Test
    public void testMapPojoInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/mapPojo", APPLICATION_JSON, createExpectedMap("mapPojo"));
    }

    @Test
    public void testMapPojoInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/mapPojo", APPLICATION_JSON);
    }

    @Test
    public void testMapPojoInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/mapPojo", APPLICATION_JSON);
    }
}
