package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BigDecimalTest extends AbstractTest {

    // Just BigDecimal
    @Test
    public void testJustBigDecimalInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustBigDecimalInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustBigDecimalInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justBigDecimal", TEXT_PLAIN);
    }

    @Test
    public void testJustBigDecimalInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justBigDecimal", TEXT_PLAIN);
    }

    // ResponseEntity<BigDecimal>
    @Test
    public void testRestResponseBigDecimalInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/responseEntityBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseBigDecimalInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/responseEntityBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseBigDecimalInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityBigDecimal", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseBigDecimalInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityBigDecimal", TEXT_PLAIN);
    }

    // Optional<BigDecimal>
    //@Test
    public void testOptionalBigDecimalInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalBigDecimal", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalBigDecimalInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalBigDecimalInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalBigDecimal", TEXT_PLAIN);
    }

    @Test
    public void testOptionalBigDecimalInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalBigDecimal", TEXT_PLAIN);
    }

    // Uni<BigDecimal>
    @Test
    public void testUniBigDecimalInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniBigDecimalInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniBigDecimal", TEXT_PLAIN);
    }

    // CompletionStage<BigDecimal>
    @Test
    public void testCompletionStageBigDecimalInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStageBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageBigDecimalInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageBigDecimal", TEXT_PLAIN);
    }

    // CompletedFuture<BigDecimal>
    @Test
    public void testCompletedFutureBigDecimalInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFutureBigDecimal", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureBigDecimalInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageBigDecimal", TEXT_PLAIN);
    }

    // List<BigDecimal>
    @Test
    public void testListBigDecimalInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listBigDecimal", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListBigDecimalInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listBigDecimal", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListBigDecimalInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listBigDecimal", APPLICATION_JSON);
    }

    @Test
    public void testListBigDecimalInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listBigDecimal", APPLICATION_JSON);
    }

    // BigDecimal[]
    @Test
    public void testArrayBigDecimalInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayBigDecimal", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayBigDecimalInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayBigDecimal", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayBigDecimalInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayBigDecimal", APPLICATION_JSON);
    }

    @Test
    public void testArrayBigDecimalInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayBigDecimal", APPLICATION_JSON);
    }

}
