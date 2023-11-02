package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LongTest extends AbstractTest {

    // Just Long
    @Test
    public void testJustLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justLong", TEXT_PLAIN);
    }

    @Test
    public void testJustLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justLong", TEXT_PLAIN);
    }

    // Just long
    @Test
    public void testJustPrimitiveLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justPrimitiveLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustPrimitiveLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justPrimitiveLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustPrimitiveLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justPrimitiveLong", TEXT_PLAIN);
    }

    @Test
    public void testJustPrimitiveLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justPrimitiveLong", TEXT_PLAIN);
    }

    // ResponseEntity<Long>
    @Test
    public void testResponseEntityLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/responseEntityLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testResponseEntityLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/responseEntityLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testResponseEntityLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityLong", TEXT_PLAIN);
    }

    @Test
    public void testResponseEntityLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityLong", TEXT_PLAIN);
    }

    // Optional<Long>
    //@Test
    public void testOptionalLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalLong", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalLong", TEXT_PLAIN);
    }

    @Test
    public void testOptionalLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalLong", TEXT_PLAIN);
    }

    // OptionalLong
    //@Test
    public void testOptionalPrimitiveLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalPrimitiveLong", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalPrimitiveLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalPrimitiveLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalPrimitiveLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalPrimitiveLong", TEXT_PLAIN);
    }

    @Test
    public void testOptionalPrimitiveLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalPrimitiveLong", TEXT_PLAIN);
    }

    // Uni<Long>
    @Test
    public void testUniLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniLong", TEXT_PLAIN);
    }

    // CompletionStage<Long>
    @Test
    public void testCompletionStageLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStageLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageLong", TEXT_PLAIN);
    }

    // CompletedFuture<Long>
    @Test
    public void testCompletedFutureLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFutureLong", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageLong", TEXT_PLAIN);
    }

    // List<Long>
    @Test
    public void testListLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listLong", APPLICATION_JSON);
    }

    @Test
    public void testListLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listLong", APPLICATION_JSON);
    }

    // Long[]
    @Test
    public void testArrayLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayLong", APPLICATION_JSON);
    }

    @Test
    public void testArrayLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayLong", APPLICATION_JSON);
    }

    // long[]
    @Test
    public void testArrayPrimitiveLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayPrimitiveLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayPrimitiveLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayPrimitiveLong", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayPrimitiveLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayPrimitiveLong", APPLICATION_JSON);
    }

    @Test
    public void testArrayPrimitiveLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayPrimitiveLong", APPLICATION_JSON);
    }

    // Map<Long, Long>
    @Test
    public void testMapLongInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/mapLong", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapLongInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/mapLong", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapLongInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/mapLong", APPLICATION_JSON);
    }

    @Test
    public void testMapLongInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/mapLong", APPLICATION_JSON);
    }

}
