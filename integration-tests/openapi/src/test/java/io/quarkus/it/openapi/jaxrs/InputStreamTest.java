package io.quarkus.it.openapi.jaxrs;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractInputStreamTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InputStreamTest extends AbstractInputStreamTest {

    // Just InputStream
    @Test
    public void testJustInputStreamInJaxRsServiceRequest() throws IOException {
        testServiceInputStreamRequest("/jax-rs/defaultContentType/justInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustInputStreamInJaxRsServiceResponse() throws IOException {
        testServiceInputStreamResponse("/jax-rs/defaultContentType/justInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustInputStreamInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustInputStreamInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // RestResponse<InputStream>
    @Test
    public void testRestResponseInputStreamInJaxRsServiceRequest() throws IOException {
        testServiceInputStreamRequest("/jax-rs/defaultContentType/restResponseInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseInputStreamInJaxRsServiceResponse() throws IOException {
        testServiceInputStreamResponse("/jax-rs/defaultContentType/restResponseInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseInputStreamInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseInputStreamInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Optional<InputStream>
    //@Test
    public void testOptionalInputStreamInJaxRsServiceRequest() throws IOException {
        testServiceInputStreamRequest("/jax-rs/defaultContentType/optionalInputStream", APPLICATION_OCTET_STREAM);
    }

    //@Test
    public void testOptionalInputStreamInJaxRsServiceResponse() throws IOException {
        testServiceInputStreamResponse("/jax-rs/defaultContentType/optionalInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalInputStreamInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalInputStreamInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Uni<InputStream>

    @Test
    public void testUniInputStreamInJaxRsServiceResponse() throws IOException {
        testServiceInputStreamResponse("/jax-rs/defaultContentType/uniInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testUniInputStreamInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletionStage<InputStream>

    @Test
    public void testCompletionStageInputStreamInJaxRsServiceResponse() throws IOException {
        testServiceInputStreamResponse("/jax-rs/defaultContentType/completionStageInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletionStageInputStreamInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletedFuture<InputStream>
    @Test
    public void testCompletedFutureInputStreamInJaxRsServiceResponse() throws IOException {
        testServiceInputStreamResponse("/jax-rs/defaultContentType/completedFutureInputStream", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletedFutureInputStreamInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageInputStream/{fileName}", APPLICATION_OCTET_STREAM);
    }
}
