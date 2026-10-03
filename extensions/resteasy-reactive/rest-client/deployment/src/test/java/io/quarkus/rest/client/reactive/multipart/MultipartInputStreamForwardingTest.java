package io.quarkus.rest.client.reactive.multipart;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MultipartInputStreamForwardingTest {

    private static final int BODY_SIZE = 4 * 1024 * 1024;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ForwardingResource.class, ReceivingResource.class, Client.class,
                    ClientForm.class));

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void forwardsIncomingBodyAsStreamedPart() {
        byte[] body = new byte[BODY_SIZE];
        for (int i = 0; i < body.length; i++) {
            body[i] = (byte) i;
        }
        given().body(body).contentType(MediaType.APPLICATION_OCTET_STREAM)
                .post("/forward")
                .then()
                .statusCode(200)
                .body(equalTo("chunked:" + BODY_SIZE + ":" + checksum(body)));
    }

    private static long checksum(byte[] bytes) {
        long sum = 0;
        for (byte b : bytes) {
            sum = sum * 31 + b;
        }
        return sum;
    }

    @Path("/forward")
    public static class ForwardingResource {

        @POST
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        public String forward(InputStream body, @Context UriInfo uriInfo) {
            Client client = RestClientBuilder.newBuilder().baseUri(uriInfo.getBaseUri()).build(Client.class);
            ClientForm form = new ClientForm();
            form.file = body;
            return client.upload(form);
        }
    }

    @Path("/receive")
    public static class ReceivingResource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String receive(@RestForm("file") FileUpload file, @RestHeader("Transfer-Encoding") String transferEncoding)
                throws IOException {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.uploadedFile());
            return ("chunked".equals(transferEncoding) ? "chunked" : "not-chunked") + ":" + bytes.length + ":"
                    + checksum(bytes);
        }
    }

    @Path("/receive")
    public interface Client {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String upload(ClientForm form);
    }

    public static class ClientForm {

        @RestForm("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream file;
    }
}
