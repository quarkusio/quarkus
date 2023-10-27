package io.quarkus.it.openapi.jaxrs;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ShortTest extends AbstractTest {

    // Just Short
    //@Test
    public void testJustShortInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustShortInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justShort", TEXT_PLAIN);
    }

    @Test
    public void testJustShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justShort", TEXT_PLAIN);
    }

    // Just long
    @Test
    public void testJustPrimitiveShortInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/justPrimitiveShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustPrimitiveShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/justPrimitiveShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustPrimitiveShortInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justPrimitiveShort", TEXT_PLAIN);
    }

    @Test
    public void testJustPrimitiveShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justPrimitiveShort", TEXT_PLAIN);
    }

    // RestResponse<Short>
    //@Test
    public void testRestResponseShortInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/restResponseShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/restResponseShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testRestResponseShortInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseShort", TEXT_PLAIN);
    }

    @Test
    public void testRestResponseShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseShort", TEXT_PLAIN);
    }

    // Optional<Short>
    //@Test
    public void testOptionalShortInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/optionalShort", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/optionalShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalShortInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalShort", TEXT_PLAIN);
    }

    @Test
    public void testOptionalShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalShort", TEXT_PLAIN);
    }

    // Uni<Short>
    @Test
    public void testUniShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/uniShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniShort", TEXT_PLAIN);
    }

    // CompletionStage<Short>
    @Test
    public void testCompletionStageShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completionStageShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageShort", TEXT_PLAIN);
    }

    // CompletedFuture<Short>
    @Test
    public void testCompletedFutureShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/completedFutureShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageShort", TEXT_PLAIN);
    }

    // List<Short>
    @Test
    public void testListShortInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/listShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/listShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListShortInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/listShort", APPLICATION_JSON);
    }

    @Test
    public void testListShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/listShort", APPLICATION_JSON);
    }

    // Short[]
    @Test
    public void testArrayShortInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayShortInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayShort", APPLICATION_JSON);
    }

    @Test
    public void testArrayShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayShort", APPLICATION_JSON);
    }

    // long[]
    @Test
    public void testArrayPrimitiveShortInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/arrayPrimitiveShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayPrimitiveShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/arrayPrimitiveShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayPrimitiveShortInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/arrayPrimitiveShort", APPLICATION_JSON);
    }

    @Test
    public void testArrayPrimitiveShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/arrayPrimitiveShort", APPLICATION_JSON);
    }

    // Map<Short, Short>
    @Test
    public void testMapShortInJaxRsServiceRequest() {
        testServiceRequest("/jax-rs/defaultContentType/mapShort", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapShortInJaxRsServiceResponse() {
        testServiceResponse("/jax-rs/defaultContentType/mapShort", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapShortInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/mapShort", APPLICATION_JSON);
    }

    @Test
    public void testMapShortInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/mapShort", APPLICATION_JSON);
    }
}
