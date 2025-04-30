package io.quarkus.it.openapi.spring;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractFileTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FileTest extends AbstractFileTest {

    // Just File
    @Test
    public void testJustFileInSpringServiceRequest() throws IOException {
        testServiceFileRequest("/spring/defaultContentType/justFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustFileInSpringServiceResponse() throws IOException {
        testServiceFileResponse("/spring/defaultContentType/justFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustFileInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustFileInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // ResponseEntity<File>
    @Test
    public void testResponseEntityFileInSpringServiceRequest() throws IOException {
        testServiceFileRequest("/spring/defaultContentType/responseEntityFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityFileInSpringServiceResponse() throws IOException {
        testServiceFileResponse("/spring/defaultContentType/responseEntityFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityFileInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityFileInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Optional<File>
    //@Test
    public void testOptionalFileInSpringServiceRequest() throws IOException {
        testServiceFileRequest("/spring/defaultContentType/optionalFile", APPLICATION_OCTET_STREAM);
    }

    //@Test
    public void testOptionalFileInSpringServiceResponse() throws IOException {
        testServiceFileResponse("/spring/defaultContentType/optionalFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalFileInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalFileInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Uni<File>

    @Test
    public void testUniFileInSpringServiceResponse() throws IOException {
        testServiceFileResponse("/spring/defaultContentType/uniFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testUniFileInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletionStage<File>

    @Test
    public void testCompletionStageFileInSpringServiceResponse() throws IOException {
        testServiceFileResponse("/spring/defaultContentType/completionStageFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletionStageFileInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletedFuture<File>
    @Test
    public void testCompletedFutureFileInSpringServiceResponse() throws IOException {
        testServiceFileResponse("/spring/defaultContentType/completedFutureFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletedFutureFileInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageFile/{fileName}", APPLICATION_OCTET_STREAM);
    }
}
