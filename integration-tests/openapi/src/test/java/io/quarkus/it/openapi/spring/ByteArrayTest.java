package io.quarkus.it.openapi.spring;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractByteArrayTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ByteArrayTest extends AbstractByteArrayTest {

    // Just byte[]
    @Test
    public void testJustByteArrayInSpringServiceRequest() throws IOException {
        testServiceByteArrayRequest("/spring/defaultContentType/justByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJusByteArrayInSpringServiceResponse() throws IOException {
        testServiceByteArrayResponse("/spring/defaultContentType/justByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustByteArrayInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/justByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustByteArrayInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/justByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // ResponseEntity<byte[]>
    @Test
    public void testResponseEntityByteArrayInSpringServiceRequest() throws IOException {
        testServiceByteArrayRequest("/spring/defaultContentType/responseEntityByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testResponseEntityByteArrayInSpringServiceResponse() throws IOException {
        testServiceByteArrayResponse("/spring/defaultContentType/responseEntityByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseByteArrayInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/responseEntityByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseByteArrayInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/responseEntityByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Optional<byte[]>
    //@Test
    public void testOptionalByteArrayInSpringServiceRequest() throws IOException {
        testServiceByteArrayRequest("/spring/defaultContentType/optionalByteArray", APPLICATION_OCTET_STREAM);
    }

    //@Test
    public void testOptionalByteArrayInSpringServiceResponse() throws IOException {
        testServiceByteArrayResponse("/spring/defaultContentType/optionalByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalByteArrayInSpringOpenAPIRequest() {
        testOpenAPIRequest("/spring/defaultContentType/optionalByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalByteArrayInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/optionalByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Uni<byte[]>

    @Test
    public void testUniByteArrayInSpringServiceResponse() throws IOException {
        testServiceByteArrayResponse("/spring/defaultContentType/uniByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testUniByteArrayInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/uniByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletionStage<byte[]>

    @Test
    public void testCompletionStageByteArrayInSpringServiceResponse() throws IOException {
        testServiceByteArrayResponse("/spring/defaultContentType/completionStageByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletionStageByteArrayInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletedFuture<byte[]>
    @Test
    public void testCompletedFutureByteArrayInSpringServiceResponse() throws IOException {
        testServiceByteArrayResponse("/spring/defaultContentType/completedFutureByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletedFutureByteArrayInSpringOpenAPIResponse() {
        testOpenAPIResponse("/spring/defaultContentType/completionStageByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }
}
