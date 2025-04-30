package io.quarkus.it.openapi.spring;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ShortTest extends AbstractTest {

    // Just Short
    //@Test
    public void testJustShortInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustShortInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justShort", TEXT_PLAIN);
    }

    @Test
    public void testJustShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justShort", TEXT_PLAIN);
    }

    // Just short
    @Test
    public void testJustPrimitiveShortInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/justPrimitiveShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustPrimitiveShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/justPrimitiveShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testJustPrimitiveShortInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justPrimitiveShort", TEXT_PLAIN);
    }

    @Test
    public void testJustPrimitiveShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justPrimitiveShort", TEXT_PLAIN);
    }

    // ResponseEntity<Short>
    //@Test
    public void testResponseEntityShortInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/responseEntityShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testResponseEntityShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/responseEntityShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testResponseEntityShortInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityShort", TEXT_PLAIN);
    }

    @Test
    public void testResponseEntityShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityShort", TEXT_PLAIN);
    }

    // Optional<Short>
    //@Test
    public void testOptionalShortInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/optionalShort", TEXT_PLAIN, "0");
    }

    //@Test
    public void testOptionalShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/optionalShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testOptionalShortInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalShort", TEXT_PLAIN);
    }

    @Test
    public void testOptionalShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalShort", TEXT_PLAIN);
    }

    // Uni<Short>
    @Test
    public void testUniShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/uniShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testUniShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniShort", TEXT_PLAIN);
    }

    // CompletionStage<Short>
    @Test
    public void testCompletionStageShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completionStageShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletionStageShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageShort", TEXT_PLAIN);
    }

    // CompletedFuture<Short>
    @Test
    public void testCompletedFutureShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/completedFutureShort", TEXT_PLAIN, "0");
    }

    @Test
    public void testCompletedFutureShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageShort", TEXT_PLAIN);
    }

    // List<Short>
    @Test
    public void testListShortInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/listShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/listShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testListShortInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/listShort", APPLICATION_JSON);
    }

    @Test
    public void testListShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/listShort", APPLICATION_JSON);
    }

    // Short[]
    @Test
    public void testArrayShortInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayShortInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayShort", APPLICATION_JSON);
    }

    @Test
    public void testArrayShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayShort", APPLICATION_JSON);
    }

    // long[]
    @Test
    public void testArrayPrimitiveShortInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/arrayPrimitiveShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayPrimitiveShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/arrayPrimitiveShort", APPLICATION_JSON, "[0]");
    }

    @Test
    public void testArrayPrimitiveShortInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/arrayPrimitiveShort", APPLICATION_JSON);
    }

    @Test
    public void testArrayPrimitiveShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/arrayPrimitiveShort", APPLICATION_JSON);
    }

    // Map<Short, Short>
    @Test
    public void testMapShortInSpringServiceRequest() {
        testServiceRequest("/spring/defaultContentType/mapShort", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapShortInSpringServiceResponse() {
        testServiceResponse("/spring/defaultContentType/mapShort", APPLICATION_JSON, "{\"0\":0}");
    }

    @Test
    public void testMapShortInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/mapShort", APPLICATION_JSON);
    }

    @Test
    public void testMapShortInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/mapShort", APPLICATION_JSON);
    }

}
