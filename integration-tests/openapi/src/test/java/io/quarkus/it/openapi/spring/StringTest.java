package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class StringTest extends AbstractTest {

    // Just String
    @Test
    public void testJustStringInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justString", TEXT_PLAIN, "justString");
    }

    @Test
    public void testJustStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justString", TEXT_PLAIN, "justString");
    }

    @Test
    public void testJustStringInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justString", TEXT_PLAIN);
    }

    @Test
    public void testJustStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justString", TEXT_PLAIN);
    }

    // ResponseEntity<String>
    @Test
    public void testResponseEntityStringInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/responseEntityString", TEXT_PLAIN, "responseEntityString");
    }

    @Test
    public void testResponseEntityStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/responseEntityString", TEXT_PLAIN, "responseEntityString");
    }

    @Test
    public void testResponseEntityStringInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityString", TEXT_PLAIN);
    }

    @Test
    public void testResponseEntityStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityString", TEXT_PLAIN);
    }

    // Optional<String>
    //@Test
    public void testOptionalStringInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalString", TEXT_PLAIN, "optionalString");
    }

    //@Test
    public void testOptionalStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalString", TEXT_PLAIN, "optionalString");
    }

    @Test
    public void testOptionalStringInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalString", TEXT_PLAIN);
    }

    @Test
    public void testOptionalStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalString", TEXT_PLAIN);
    }

    // Uni<String>
    @Test
    public void testUniStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniString", TEXT_PLAIN, "uniString");
    }

    @Test
    public void testUniStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniString", TEXT_PLAIN);
    }

    // CompletionStage<String>
    @Test
    public void testCompletionStageStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStageString", TEXT_PLAIN, "completionStageString");
    }

    @Test
    public void testCompletionStageStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageString", TEXT_PLAIN);
    }

    // CompletedFuture<String>
    @Test
    public void testCompletedFutureStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFutureString", TEXT_PLAIN, "completedFutureString");
    }

    @Test
    public void testCompletedFutureStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completedFutureString", TEXT_PLAIN);
    }

    // List<String>
    @Test
    public void testListStringInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listString", APPLICATION_JSON, "[\"listString\"]");
    }

    @Test
    public void testListStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listString", APPLICATION_JSON, "[\"listString\"]");
    }

    @Test
    public void testListStringInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listString", APPLICATION_JSON);
    }

    @Test
    public void testListStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listString", APPLICATION_JSON);
    }

    // String[]
    @Test
    public void testArrayStringInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayString", APPLICATION_JSON, "[\"arrayString\"]");
    }

    @Test
    public void testArrayStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayString", APPLICATION_JSON, "[\"arrayString\"]");
    }

    @Test
    public void testArrayStringInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayString", APPLICATION_JSON);
    }

    @Test
    public void testArrayStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayString", APPLICATION_JSON);
    }

    // Map<String,String>
    @Test
    public void testMapStringInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/mapString", APPLICATION_JSON, "{\"mapString\":\"mapString\"}");
    }

    @Test
    public void testMapStringInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/mapString", APPLICATION_JSON, "{\"mapString\":\"mapString\"}");
    }

    @Test
    public void testMapStringInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/mapString", APPLICATION_JSON);
    }

    @Test
    public void testMapStringInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/mapString", APPLICATION_JSON);
    }
}
