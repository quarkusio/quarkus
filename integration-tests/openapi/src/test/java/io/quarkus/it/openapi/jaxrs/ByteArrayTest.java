package io.quarkus.it.openapi.jaxrs;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.it.openapi.AbstractByteArrayTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ByteArrayTest extends AbstractByteArrayTest {

    // Just byte[]
    @Test
    public void testJustByteArrayInJaxRsServiceRequest() throws IOException {
        testServiceByteArrayRequest("/jax-rs/defaultContentType/justByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJusByteArrayInJaxRsServiceResponse() throws IOException {
        testServiceByteArrayResponse("/jax-rs/defaultContentType/justByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustByteArrayInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/justByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testJustByteArrayInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/justByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // RestResponse<byte[]>
    @Test
    public void testRestResponseByteArrayInJaxRsServiceRequest() throws IOException {
        testServiceByteArrayRequest("/jax-rs/defaultContentType/restResponseByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseByteArrayInJaxRsServiceResponse() throws IOException {
        testServiceByteArrayResponse("/jax-rs/defaultContentType/restResponseByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseByteArrayInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/restResponseByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testRestResponseByteArrayInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/restResponseByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Optional<byte[]>
    //@Test
    public void testOptionalByteArrayInJaxRsServiceRequest() throws IOException {
        testServiceByteArrayRequest("/jax-rs/defaultContentType/optionalByteArray", APPLICATION_OCTET_STREAM);
    }

    //@Test
    public void testOptionalByteArrayInJaxRsServiceResponse() throws IOException {
        testServiceByteArrayResponse("/jax-rs/defaultContentType/optionalByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalByteArrayInJaxRsOpenAPIRequest() {
        testOpenAPIRequest("/jax-rs/defaultContentType/optionalByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testOptionalByteArrayInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/optionalByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // Uni<byte[]>

    @Test
    public void testUniByteArrayInJaxRsServiceResponse() throws IOException {
        testServiceByteArrayResponse("/jax-rs/defaultContentType/uniByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testUniByteArrayInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/uniByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletionStage<byte[]>

    @Test
    public void testCompletionStageByteArrayInJaxRsServiceResponse() throws IOException {
        testServiceByteArrayResponse("/jax-rs/defaultContentType/completionStageByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletionStageByteArrayInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }

    // CompletedFuture<byte[]>
    @Test
    public void testCompletedFutureByteArrayInJaxRsServiceResponse() throws IOException {
        testServiceByteArrayResponse("/jax-rs/defaultContentType/completedFutureByteArray", APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testCompletedFutureByteArrayInJaxRsOpenAPIResponse() {
        testOpenAPIResponse("/jax-rs/defaultContentType/completionStageByteArray/{fileName}", APPLICATION_OCTET_STREAM);
    }
}
