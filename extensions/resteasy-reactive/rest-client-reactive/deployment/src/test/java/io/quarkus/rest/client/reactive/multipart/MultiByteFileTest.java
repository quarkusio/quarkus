package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.Vertx;

public class MultiByteFileTest {
    public static final int BYTES_SENT = 5_000_000; // 5 megs

    private static final Logger log = Logger.getLogger(MultiByteFileTest.class);

    @TestHTTPResource
    URI baseUri;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class, FormData.class, Client.class, ClientForm.class));

    @Test
    void shouldUploadBiggishFile() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientForm form = new ClientForm();
        AtomicLong i = new AtomicLong();
        form.file = Multi.createBy().repeating().supplier(
                () -> (byte) ((i.getAndIncrement() + 1) % 123)).atMost(BYTES_SENT);
        String result = client.postMultipart(form);
        assertThat(result).isEqualTo("myFile");
    }

    @Test
    void shouldUploadTwoSmallFiles() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientFormWithTwoFiles form = new ClientFormWithTwoFiles();

        form.file1 = Multi.createBy().repeating().supplier(() -> (byte) 4).atMost(100);
        form.file2 = Multi.createBy().repeating().supplier(() -> (byte) 7).atMost(100);

        String result = client.postMultipartWithTwoFiles(form);
        assertThat(result).isEqualTo("myFile1myFile2");
    }

    @Test
    void shouldUploadSlowlyProducedData() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientForm form = new ClientForm();
        AtomicLong i = new AtomicLong();
        form.file = Multi.createFrom().emitter(em -> delayedEmit(i, em));
        String result = client.postMultipartFromSlowMulti(form);
        assertThat(result).isEqualTo("myFile");
    }

    @Test
    void shouldWorkOnNullMulti() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientForm form = new ClientForm();
        form.file = null;
        form.param = "some-param";
        String result = client.postNull(form);
        assertThat(result).isEqualTo("NULL_FILE");
    }

    @Test
    void shouldWorkOnEmptyMulti() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientForm form = new ClientForm();
        form.file = Multi.createFrom().items();
        String result = client.postEmpty(form);
        assertThat(result).isEqualTo("myFile");
    }

    @Test
    @Timeout(10)
    void shouldBehaveWellWithErrorOnMulti() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientForm form = new ClientForm();
        AtomicLong i = new AtomicLong();
        form.file = Multi.createBy().repeating().supplier(
                () -> {
                    long iteration = i.getAndIncrement();
                    if (iteration > BYTES_SENT / 2) {
                        throw new RuntimeException("forced");
                    }
                    return (byte) ((iteration + 1) % 123);
                }).atMost(BYTES_SENT);
        assertThatThrownBy(() -> client.postMultipart(form)).isInstanceOf(Exception.class);
    }

    @Test
    void shouldSendFromMultiEmittingOutsideEventLoop() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientForm form = new ClientForm();
        AtomicLong i = new AtomicLong();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        form.file = Multi.createBy().repeating().supplier(
                () -> (byte) ((i.getAndIncrement() + 1) % 123)).atMost(BYTES_SENT)
                .emitOn(executor);
        String result = client.postMultipart(form);
        assertThat(result).isEqualTo("myFile");
    }

    private void delayedEmit(AtomicLong i, MultiEmitter<? super Byte> em) {
        vertx.setTimer(100, id -> {
            long index = i.getAndIncrement();
            if (index < 10) {
                em.emit((byte) (12 + index)); // should emit bytes from 12 to 22
                delayedEmit(i, em);
            } else {
                em.complete();
            }
        });
    }

    @Path("/multipart")
    @ApplicationScoped
    public static class Resource {
        @Path("/from-slow")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadFromSlow(@MultipartForm FormData form) {
            return verifyFile(form.myFile, 10, position -> (byte) (12 + position));
        }

        @Path("/null")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadNull(@MultipartForm FormDataWithFile form) {
            return form.myFile != null ? "NON_NULL_FILE_FROM_NULL_MULTI" : "NULL_FILE";
        }

        @Path("/")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String upload(@MultipartForm FormData form) {
            return verifyFile(form.myFile, BYTES_SENT, position -> (byte) (((1 + position) % 123)));
        }

        @Path("/empty")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadEmpty(@MultipartForm FormData form) {
            return verifyFile(form.myFile, 0, b -> (byte) 0);
        }

        @Path("/two-files")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadEmpty(@MultipartForm FormWithTwoFiles form) {
            return verifyFile(form.file1, 100, position -> (byte) 4)
                    + verifyFile(form.file2, 100, position -> (byte) 7);
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
            return upload.fileName();
        }
    }

    public static class FormData {
        @FormParam("myFile")
        public FileUpload myFile;
        @FormParam("myParam")
        @PartType(MediaType.TEXT_PLAIN)
        public String myParam;
    }

    public static class FormDataWithFile {
        @FormParam("myFile")
        public File myFile;
        @FormParam("myParam")
        @PartType(MediaType.TEXT_PLAIN)
        public String myParam;
    }

    public static class FormWithTwoFiles {
        @FormParam("myFile1")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public FileUpload file1;
        @FormParam("myFile2")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public FileUpload file2;
    }

    @Path("/multipart")
    public interface Client {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipart(@MultipartForm ClientForm clientForm);

        @Path("/two-files")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithTwoFiles(@MultipartForm ClientFormWithTwoFiles twoFiles);

        @Path("/from-slow")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartFromSlowMulti(@MultipartForm ClientForm clientForm);

        @Path("/null")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postNull(@MultipartForm ClientForm clientForm);

        @Path("/empty")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postEmpty(@MultipartForm ClientForm form);
    }

    public static class ClientForm {

        @FormParam("myParam")
        @PartType(MediaType.TEXT_PLAIN)
        public String param;
        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public Multi<Byte> file;
    }

    public static class ClientFormWithTwoFiles {
        @FormParam("myFile1")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public Multi<Byte> file1;
        @FormParam("myFile2")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public Multi<Byte> file2;
    }
}
