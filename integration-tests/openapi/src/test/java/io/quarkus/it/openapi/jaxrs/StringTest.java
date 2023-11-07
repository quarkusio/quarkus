package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class StringTest extends AbstractTest {

    // Just String
    @Test
    public void testJustStringInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justString", TEXT_PLAIN, "justString");
    }

    @Test
    public void testJustStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justString", TEXT_PLAIN, "justString");
    }

    @Test
    public void testJustStringInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justString", TEXT_PLAIN);
    }

    @Test
    public void testJustStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justString", TEXT_PLAIN);
    }

    // RestResponse<String>
    @Test
    public void testRestResponseStringInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponseString", TEXT_PLAIN, "restResponseString");
    }

    @Test
    public void testRestResponseStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponseString", TEXT_PLAIN, "restResponseString");
    }

    @Test
    public void testRestResponseStringInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseString", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseString", TEXT_PLAIN);
    }

    // Optional<String>
    // @Test
    public void testOptionalStringInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalString", TEXT_PLAIN, "optionalString");
    }

    // @Test
    public void testOptionalStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalString", TEXT_PLAIN, "optionalString");
    }

    @Test
    public void testOptionalStringInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalString", TEXT_PLAIN);
    }

    @Test
    public void testOptionalStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalString", TEXT_PLAIN);
    }

    // Uni<String>
    @Test
    public void testUniStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniString", TEXT_PLAIN, "uniString");
    }

    @Test
    public void testUniStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniString", TEXT_PLAIN);
    }

    // CompletionStage<String>
    @Test
    public void testCompletionStageStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStageString", TEXT_PLAIN, "completionStageString");
    }

    @Test
    public void testCompletionStageStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageString", TEXT_PLAIN);
    }

    // CompletedFuture<String>
    @Test
    public void testCompletedFutureStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFutureString", TEXT_PLAIN, "completedFutureString");
    }

    @Test
    public void testCompletedFutureStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completedFutureString", TEXT_PLAIN);
    }

    // List<String>
    @Test
    public void testListStringInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listString", APPLICATION_JSON, "[\"listString\"]");
    }

    @Test
    public void testListStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listString", APPLICATION_JSON, "[\"listString\"]");
    }

    @Test
    public void testListStringInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listString", APPLICATION_JSON);
    }

    @Test
    public void testListStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listString", APPLICATION_JSON);
    }

    // String[]
    @Test
    public void testArrayStringInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayString", APPLICATION_JSON, "[\"arrayString\"]");
    }

    @Test
    public void testArrayStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayString", APPLICATION_JSON, "[\"arrayString\"]");
    }

    @Test
    public void testArrayStringInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayString", APPLICATION_JSON);
    }

    @Test
    public void testArrayStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayString", APPLICATION_JSON);
    }

    // Map<String,String>
    @Test
    public void testMapStringInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/mapString", APPLICATION_JSON, "{\"mapString\":\"mapString\"}");
    }

    @Test
    public void testMapStringInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/mapString", APPLICATION_JSON, "{\"mapString\":\"mapString\"}");
    }

    @Test
    public void testMapStringInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/mapString", APPLICATION_JSON);
    }

    @Test
    public void testMapStringInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/mapString", APPLICATION_JSON);
    }
}
