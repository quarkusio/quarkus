package io.quarkus.it.openapi.jaxrs;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractReaderTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ReaderTest extends AbstractReaderTest {

    // Just Reader
    @Test
    public void testJustReaderInJaxRsServiceRequest() throws IOException {
        testServiceReaderRequest("/jax-rs/defaultContentType/justReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJusReaderInJaxRsServiceResponse() throws IOException {
        testServiceReaderResponse("/jax-rs/defaultContentType/justReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustReaderInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustReaderInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // RestResponse<Reader>
    @Test
    public void testRestResponseReaderInJaxRsServiceRequest() throws IOException {
        testServiceReaderRequest("/jax-rs/defaultContentType/restResponseReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseReaderInJaxRsServiceResponse() throws IOException {
        testServiceReaderResponse("/jax-rs/defaultContentType/restResponseReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseReaderInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseReaderInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Optional<Reader>
    //@Test
    public void testOptionalReaderInJaxRsServiceRequest() throws IOException {
        testServiceReaderRequest("/jax-rs/defaultContentType/optionalReader", APPLICATION_OCTET_STREAM);
    }

    //@Test
    public void testOptionalReaderInJaxRsServiceResponse() throws IOException {
        testServiceReaderResponse("/jax-rs/defaultContentType/optionalReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalReaderInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalReaderInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Uni<Reader>

    @Test
    public void testUniReaderInJaxRsServiceResponse() throws IOException {
        testServiceReaderResponse("/jax-rs/defaultContentType/uniReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testUniReaderInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletionStage<Reader>

    @Test
    public void testCompletionStageReaderInJaxRsServiceResponse() throws IOException {
        testServiceReaderResponse("/jax-rs/defaultContentType/completionStageReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletionStageReaderInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletedFuture<Reader>
    @Test
    public void testCompletedFutureReaderInJaxRsServiceResponse() throws IOException {
        testServiceReaderResponse("/jax-rs/defaultContentType/completedFutureReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletedFutureReaderInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageReader/{fileName}", APPLICATION_OCTET_STREAM);
    }
}
