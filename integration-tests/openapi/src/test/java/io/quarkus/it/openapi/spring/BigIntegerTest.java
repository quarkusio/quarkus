package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BigIntegerTest extends AbstractTest {

    // Just BigInteger
    @Test
    public void testJustBigIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustBigIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustBigIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justBigInteger", TEXT_PLAIN);
    }

    @Test
    public void testJustBigIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justBigInteger", TEXT_PLAIN);
    }

    // ResponseEntity<BigInteger>
    @Test
    public void testRestResponseBigIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/responseEntityBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseBigIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/responseEntityBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseBigIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityBigInteger", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseBigIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityBigInteger", TEXT_PLAIN);
    }

    // Optional<BigInteger>
    //@Test
    public void testOptionalBigIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalBigInteger", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalBigIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalBigIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalBigInteger", TEXT_PLAIN);
    }

    @Test
    public void testOptionalBigIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalBigInteger", TEXT_PLAIN);
    }

    // Uni<BigInteger>
    @Test
    public void testUniBigIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniBigIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniBigInteger", TEXT_PLAIN);
    }

    // CompletionStage<BigInteger>
    @Test
    public void testCompletionStageBigIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStageBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageBigIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageBigInteger", TEXT_PLAIN);
    }

    // CompletedFuture<BigInteger>
    @Test
    public void testCompletedFutureBigIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFutureBigInteger", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureBigIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageBigInteger", TEXT_PLAIN);
    }

    // List<BigInteger>
    @Test
    public void testListBigIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listBigInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListBigIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listBigInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListBigIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listBigInteger", APPLICATION_JSON);
    }

    @Test
    public void testListBigIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listBigInteger", APPLICATION_JSON);
    }

    // BigInteger[]
    @Test
    public void testArrayBigIntegerInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayBigInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayBigIntegerInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayBigInteger", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayBigIntegerInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayBigInteger", APPLICATION_JSON);
    }

    @Test
    public void testArrayBigIntegerInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayBigInteger", APPLICATION_JSON);
    }

}
