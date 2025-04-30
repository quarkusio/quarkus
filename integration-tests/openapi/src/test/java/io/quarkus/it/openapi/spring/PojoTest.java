package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PojoTest extends AbstractTest {

    // Just Pojo
    @Test
    public void testJustPojoInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justPojo", APPLICATION_JSON, createExpected("justPojo"));
    }

    @Test
    public void testJustPojoInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justPojo", APPLICATION_JSON, createExpected("justPojo"));
    }

    @Test
    public void testJustPojoInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justPojo", APPLICATION_JSON);
    }

    @Test
    public void testJustPojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justPojo", APPLICATION_JSON);
    }

    // ResponseEntity<Pojo>
    @Test
    public void testResponseEntityPojoInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/responseEntityPojo", APPLICATION_JSON,
                createExpected("responseEntityPojo"));
    }

    @Test
    public void testResponseEntityPojoInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/responseEntityPojo", APPLICATION_JSON,
                createExpected("responseEntityPojo"));
    }

    @Test
    public void testResponseEntityPojoInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityPojo", APPLICATION_JSON);
    }

    @Test
    public void testResponseEntityPojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityPojo", APPLICATION_JSON);
    }

    // Optional<Pojo>
    @Test
    public void testOptionalPojoInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalPojo", APPLICATION_JSON, createExpected("optionalPojo"));
    }

    @Test
    public void testOptionalPojoInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalPojo", APPLICATION_JSON, createExpected("optionalPojo"));
    }

    @Test
    public void testOptionalPojoInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalPojo", APPLICATION_JSON);
    }

    @Test
    public void testOptionalPojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalPojo", APPLICATION_JSON);
    }

    // Uni<Pojo>
    @Test
    public void testUniPojoInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniPojo", APPLICATION_JSON, createExpected("uniPojo"));
    }

    @Test
    public void testUniPojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniPojo", APPLICATION_JSON);
    }

    // CompletionStage<Pojo>
    @Test
    public void testCompletionStagePojoInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStagePojo", APPLICATION_JSON,
                createExpected("completionStagePojo"));
    }

    @Test
    public void testCompletionStagePojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStagePojo", APPLICATION_JSON);
    }

    // CompletedFuture<Pojo>
    @Test
    public void testCompletedFuturePojoInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFuturePojo", APPLICATION_JSON,
                createExpected("completedFuturePojo"));
    }

    @Test
    public void testCompletedFuturePojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completedFuturePojo", APPLICATION_JSON);
    }

    // List<Pojo>
    @Test
    public void testListPojoInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listPojo", APPLICATION_JSON, createExpectedList("listPojo"));
    }

    @Test
    public void testListPojoInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listPojo", APPLICATION_JSON, createExpectedList("listPojo"));
    }

    @Test
    public void testListPojoInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listPojo", APPLICATION_JSON);
    }

    @Test
    public void testListPojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listPojo", APPLICATION_JSON);
    }

    // Pojo[]
    @Test
    public void testArrayPojoInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayPojo", APPLICATION_JSON, createExpectedList("arrayPojo"));
    }

    @Test
    public void testArrayPojoInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayPojo", APPLICATION_JSON, createExpectedList("arrayPojo"));
    }

    @Test
    public void testArrayPojoInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayPojo", APPLICATION_JSON);
    }

    @Test
    public void testArrayPojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayPojo", APPLICATION_JSON);
    }

    // Map<String, Greeting>
    @Test
    public void testMapPojoInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/mapPojo", APPLICATION_JSON, createExpectedMap("mapPojo"));
    }

    @Test
    public void testMapPojoInSpringServiceResponse() {

        testServiceResponse("/spring/defaultContentType/mapPojo", APPLICATION_JSON, createExpectedMap("mapPojo"));
    }

    @Test
    public void testMapPojoInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/mapPojo", APPLICATION_JSON);
    }

    @Test
    public void testMapPojoInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/mapPojo", APPLICATION_JSON);
    }

}
