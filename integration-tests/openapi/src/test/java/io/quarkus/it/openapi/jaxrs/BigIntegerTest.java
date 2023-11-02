package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BigIntegerTest extends AbstractTest {

    // Just BigInteger
    @Test
    public void testJustBigIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustBigIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustBigIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justBigInteger", TEXT_PLAIN);
    }

    @Test
    public void testJustBigIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justBigInteger", TEXT_PLAIN);
    }

    // RestResponse<BigInteger>
    @Test
    public void testRestResponseBigIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponseBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseBigIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponseBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseBigIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseBigInteger", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseBigIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseBigInteger", TEXT_PLAIN);
    }

    // Optional<BigInteger>
    //@Test
    public void testOptionalBigIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalBigInteger", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalBigIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalBigIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalBigInteger", TEXT_PLAIN);
    }

    @Test
    public void testOptionalBigIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalBigInteger", TEXT_PLAIN);
    }

    // Uni<BigInteger>
    @Test
    public void testUniBigIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniBigIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniBigInteger", TEXT_PLAIN);
    }

    // CompletionStage<BigInteger>
    @Test
    public void testCompletionStageBigIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStageBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageBigIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageBigInteger", TEXT_PLAIN);
    }

    // CompletedFuture<BigInteger>
    @Test
    public void testCompletedFutureBigIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFutureBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureBigIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageBigInteger", TEXT_PLAIN);
    }

    // List<BigInteger>
    @Test
    public void testListBigIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listBigInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListBigIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listBigInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListBigIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listBigInteger", APPLICATION_JSON);
    }

    @Test
    public void testListBigIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listBigInteger", APPLICATION_JSON);
    }

    // BigInteger[]
    @Test
    public void testArrayBigIntegerInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayBigInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayBigIntegerInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayBigInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayBigIntegerInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayBigInteger", APPLICATION_JSON);
    }

    @Test
    public void testArrayBigIntegerInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayBigInteger", APPLICATION_JSON);
    }

}
