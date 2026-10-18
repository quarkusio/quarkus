package io.quarkus.it.openapi.spring;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractInputStreamTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InputStreamTest extends AbstractInputStreamTest {

    // Just InputStream
    @Test
    public void testJustInputStreamInSpringServiceRequest() throws IOException {
        testServiceInputStreamRequest("/spring/defaultContentType/justInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJusInputStreamInSpringServiceResponse() throws IOException {
        testServiceInputStreamResponse("/spring/defaultContentType/justInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustInputStreamInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustInputStreamInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // ResponseEntity<InputStream>
    @Test
    public void testResponseEntityInputStreamInSpringServiceRequest() throws IOException {
        testServiceInputStreamRequest("/spring/defaultContentType/responseEntityInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityInputStreamInSpringServiceResponse() throws IOException {
        testServiceInputStreamResponse("/spring/defaultContentType/responseEntityInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityInputStreamInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityInputStreamInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Optional<InputStream>
    //@Test
    public void testOptionalInputStreamInSpringServiceRequest() throws IOException {
        testServiceInputStreamRequest("/spring/defaultContentType/optionalInputStream", APPLICATION_OCTET_STREAM);
    }

    //@Test
    public void testOptionalInputStreamInSpringServiceResponse() throws IOException {
        testServiceInputStreamResponse("/spring/defaultContentType/optionalInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalInputStreamInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalInputStreamInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Uni<InputStream>

    @Test
    public void testUniInputStreamInSpringServiceResponse() throws IOException {
        testServiceInputStreamResponse("/spring/defaultContentType/uniInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testUniInputStreamInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletionStage<InputStream>

    @Test
    public void testCompletionStageInputStreamInSpringServiceResponse() throws IOException {
        testServiceInputStreamResponse("/spring/defaultContentType/completionStageInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletionStageInputStreamInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletedFuture<InputStream>
    @Test
    public void testCompletedFutureInputStreamInSpringServiceResponse() throws IOException {
        testServiceInputStreamResponse("/spring/defaultContentType/completedFutureInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletedFutureInputStreamInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }
}
