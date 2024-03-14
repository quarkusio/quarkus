package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;

public class MultipartProgrammaticTest {

    private static final Logger log = Logger.getLogger(MultipartProgrammaticTest.class);

    private static final int BYTES_SENT = 5_000_000; // 5 megs

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class, FormData.class, Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldUploadBiggishFile() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        AtomicLong i = new AtomicLong();
        Multi<Byte> content = Multi.createBy().repeating().supplier(
                () -> (byte) ((i.getAndIncrement() + 1) % 123)).atMost(BYTES_SENT);
        String result = client.postMultipart(ClientMultipartForm.create()
                .multiAsBinaryFileUpload("fileFormName", "fileName", content, MediaType.APPLICATION_OCTET_STREAM)
                .stringFileUpload("otherFormName", "whatever", "test", MediaType.TEXT_PLAIN));
        assertThat(result).isEqualTo("fileFormName/fileName-test");
    }

    @Path("/multipart")
    public interface Client {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipart(ClientMultipartForm form);
    }

    @Path("/multipart")
    public static class Resource {
        @Path("/")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String upload(FormData form) {
            return verifyFile(form.file, BYTES_SENT, position -> (byte) (((1 + position) % 123))) + "-" + form.other;
        }

        private String verifyFile(FileUpload upload, int expectedSize, Function<Integer, Byte> expectedByte) {
            var uploadedFile = upload.uploadedFile();
            int size;

            try (FileInputStream inputStream = new FileInputStream(uploadedFile.toFile())) {
                int position = 0;
                int b;
                while ((b = inputStream.read()) != -1) {
                    long expected = expectedByte.apply(position);
                    position++;
                    if (expected != b) {
                        throw new RuntimeException(
                                "WRONG_BYTE_READ at pos " + (position - 1) + " expected: " + expected + " got: " + b);
                    }
                }
                size = position;
            } catch (RuntimeException e) {
                return e.getMessage();
            } catch (Exception e) {
                log.error("Unexpected error in the test resource", e);
                return "UNEXPECTED ERROR";
            }

            if (size != expectedSize) {
                return "READ_WRONG_AMOUNT_OF_BYTES " + size;
            }
            return upload.name() + "/" + upload.fileName();
        }
    }

    public static class FormData {
        @FormParam("fileFormName")
        public FileUpload file;

        @FormParam("otherFormName")
        public String other;
    }
}
