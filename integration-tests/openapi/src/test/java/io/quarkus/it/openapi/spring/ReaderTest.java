package io.quarkus.it.openapi.spring;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractReaderTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ReaderTest extends AbstractReaderTest {

    // Just Reader
    @Test
    public void testJustReaderInSpringServiceRequest() throws IOException {
        testServiceReaderRequest("/spring/defaultContentType/justReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJusReaderInSpringServiceResponse() throws IOException {
        testServiceReaderResponse("/spring/defaultContentType/justReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustReaderInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustReaderInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // ResponseEntity<Reader>
    @Test
    public void testResponseEntityReaderInSpringServiceRequest() throws IOException {
        testServiceReaderRequest("/spring/defaultContentType/responseEntityReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityReaderInSpringServiceResponse() throws IOException {
        testServiceReaderResponse("/spring/defaultContentType/responseEntityReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityReaderInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityReaderInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Optional<Reader>
    //@Test
    public void testOptionalReaderInSpringServiceRequest() throws IOException {
        testServiceReaderRequest("/spring/defaultContentType/optionalReader", APPLICATION_OCTET_STREAM);
    }

    //@Test
    public void testOptionalReaderInSpringServiceResponse() throws IOException {
        testServiceReaderResponse("/spring/defaultContentType/optionalReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalReaderInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalReaderInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Uni<Reader>

    @Test
    public void testUniReaderInSpringServiceResponse() throws IOException {
        testServiceReaderResponse("/spring/defaultContentType/uniReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testUniReaderInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletionStage<Reader>

    @Test
    public void testCompletionStageReaderInSpringServiceResponse() throws IOException {
        testServiceReaderResponse("/spring/defaultContentType/completionStageReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletionStageReaderInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageReader/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletedFuture<Reader>
    @Test
    public void testCompletedFutureReaderInSpringServiceResponse() throws IOException {
        testServiceReaderResponse("/spring/defaultContentType/completedFutureReader", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletedFutureReaderInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageReader/{fileName}", APPLICATION_OCTET_STREAM);
    }
}
