package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BigDecimalTest extends AbstractTest {

    // Just BigDecimal
    @Test
    public void testJustBigDecimalInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustBigDecimalInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustBigDecimalInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justBigDecimal", TEXT_PLAIN);
    }

    @Test
    public void testJustBigDecimalInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justBigDecimal", TEXT_PLAIN);
    }

    // RestResponse<BigDecimal>
    @Test
    public void testRestResponseBigDecimalInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponseBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseBigDecimalInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponseBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseBigDecimalInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseBigDecimal", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseBigDecimalInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseBigDecimal", TEXT_PLAIN);
    }

    // Optional<BigDecimal>
    //@Test
    public void testOptionalBigDecimalInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalBigDecimal", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalBigDecimalInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalBigDecimalInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalBigDecimal", TEXT_PLAIN);
    }

    @Test
    public void testOptionalBigDecimalInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalBigDecimal", TEXT_PLAIN);
    }

    // Uni<BigDecimal>
    @Test
    public void testUniBigDecimalInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniBigDecimalInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniBigDecimal", TEXT_PLAIN);
    }

    // CompletionStage<BigDecimal>
    @Test
    public void testCompletionStageBigDecimalInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStageBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageBigDecimalInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageBigDecimal", TEXT_PLAIN);
    }

    // CompletedFuture<BigDecimal>
    @Test
    public void testCompletedFutureBigDecimalInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFutureBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureBigDecimalInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageBigDecimal", TEXT_PLAIN);
    }

    // List<BigDecimal>
    @Test
    public void testListBigDecimalInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listBigDecimal", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListBigDecimalInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listBigDecimal", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListBigDecimalInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listBigDecimal", APPLICATION_JSON);
    }

    @Test
    public void testListBigDecimalInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listBigDecimal", APPLICATION_JSON);
    }

    // BigDecimal[]
    @Test
    public void testArrayBigDecimalInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayBigDecimal", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayBigDecimalInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayBigDecimal", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayBigDecimalInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayBigDecimal", APPLICATION_JSON);
    }

    @Test
    public void testArrayBigDecimalInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayBigDecimal", APPLICATION_JSON);
    }

}
