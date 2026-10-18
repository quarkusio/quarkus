package io.quarkus.it.openapi.jaxrs;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractFileTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FileTest extends AbstractFileTest {

    // Just File
    @Test
    public void testJustFileInJaxRsServiceRequest() throws IOException {
        testServiceFileRequest("/jax-rs/defaultContentType/justFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustFileInJaxRsServiceResponse() throws IOException {
        testServiceFileResponse("/jax-rs/defaultContentType/justFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustFileInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustFileInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // RestResponse<File>
    @Test
    public void testRestResponseFileInJaxRsServiceRequest() throws IOException {
        testServiceFileRequest("/jax-rs/defaultContentType/restResponseFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseFileInJaxRsServiceResponse() throws IOException {
        testServiceFileResponse("/jax-rs/defaultContentType/restResponseFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseFileInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseFileInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Optional<File>
    //@Test
    public void testOptionalFileInJaxRsServiceRequest() throws IOException {
        testServiceFileRequest("/jax-rs/defaultContentType/optionalFile", APPLICATION_OCTET_STREAM);
    }

    //@Test
    public void testOptionalFileInJaxRsServiceResponse() throws IOException {
        testServiceFileResponse("/jax-rs/defaultContentType/optionalFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalFileInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalFileInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Uni<File>

    @Test
    public void testUniFileInJaxRsServiceResponse() throws IOException {
        testServiceFileResponse("/jax-rs/defaultContentType/uniFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testUniFileInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletionStage<File>

    @Test
    public void testCompletionStageFileInJaxRsServiceResponse() throws IOException {
        testServiceFileResponse("/jax-rs/defaultContentType/completionStageFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletionStageFileInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageFile/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletedFuture<File>
    @Test
    public void testCompletedFutureFileInJaxRsServiceResponse() throws IOException {
        testServiceFileResponse("/jax-rs/defaultContentType/completedFutureFile", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletedFutureFileInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageFile/{fileName}", APPLICATION_OCTET_STREAM);
    }
}
