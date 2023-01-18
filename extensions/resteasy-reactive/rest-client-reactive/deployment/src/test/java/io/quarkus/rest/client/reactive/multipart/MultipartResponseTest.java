package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;

public class MultipartResponseTest {

    public static final String WOO_HOO_WOO_HOO_HOO = "Woo hoo, woo hoo hoo";
    private static final long ONE_GIGA = 1024l * 1024l * 1024l * 1l;

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Test
    void shouldParseMultipartResponse() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultipartData data = client.getFile();
        assertThat(data.file).exists();
        verifyWooHooFile(data.file, 10000);
        assertThat(data.name).isEqualTo("foo");
        assertThat(data.panda.weight).isEqualTo("huge");
        assertThat(data.panda.height).isEqualTo("medium");
        assertThat(data.panda.mood).isEqualTo("happy");
        assertThat(data.number).isEqualTo(1984);
        assertThat(data.numberz).containsSequence(2008, 2011, 2014);
    }

    @Test
    void shouldParseUniMultipartResponse() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultipartData data = client.uniGetFile().await().atMost(Duration.ofSeconds(10));
        assertThat(data.file).exists();
        verifyWooHooFile(data.file, 10000);
        assertThat(data.name).isEqualTo("foo");
        assertThat(data.panda.weight).isEqualTo("huge");
        assertThat(data.panda.height).isEqualTo("medium");
        assertThat(data.panda.mood).isEqualTo("happy");
        assertThat(data.number).isEqualTo(1984);
        assertThat(data.numberz).containsSequence(2008, 2011, 2014);
    }

    @Test
    void shouldParseCompletionStageMultipartResponse() throws ExecutionException, InterruptedException, TimeoutException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultipartData data = client.csGetFile().toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(data.file).exists();
        verifyWooHooFile(data.file, 10000);
        assertThat(data.name).isEqualTo("foo");
        assertThat(data.panda.weight).isEqualTo("huge");
        assertThat(data.panda.height).isEqualTo("medium");
        assertThat(data.panda.mood).isEqualTo("happy");
        assertThat(data.number).isEqualTo(1984);
        assertThat(data.numberz).containsSequence(2008, 2011, 2014);
    }

    @Test
    void shouldParseMultipartResponseWithSmallFile() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultipartData data = client.getSmallFile();
        assertThat(data.file).exists();
        verifyWooHooFile(data.file, 1);
        assertThat(data.name).isEqualTo("foo");
        assertThat(data.panda).isNull();
        assertThat(data.number).isEqualTo(1984);
        assertThat(data.numberz).isNull();
    }

    @EnabledIfSystemProperty(named = "test-resteasy-reactive-large-files", matches = "true")
    @Test
    void shouldParseMultipartResponseWithLargeFile() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultipartData data = client.getLargeFile();
        assertThat(data.file).exists();
        assertThat(data.file.length()).isEqualTo(ONE_GIGA);
    }

    @Test
    void shouldParseMultipartResponseWithNulls() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultipartData data = client.getFileEmpty();
        assertThat(data.file).isNull();
        assertThat(data.name).isNull();
        assertThat(data.panda).isNull();
    }

    @Test
    void shouldParseMultipartResponseWithSetters() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultipartDataWithSetters data = client.getFileWithSetters();
        assertThat(data.file).exists();
        assertThat(data.name).isEqualTo("foo");
        assertThat(data.panda.weight).isEqualTo("huge");
        assertThat(data.panda.height).isEqualTo("medium");
        assertThat(data.panda.mood).isEqualTo("happy");
    }

    @Test
    void shouldBeSaneOnServerError() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThatThrownBy(client::error).isInstanceOf(ClientWebApplicationException.class);
    }

    @Test
    void shouldParseMultipartResponseWithClientBuilderApi() {
        jakarta.ws.rs.client.Client client = ClientBuilder.newBuilder().build();
        MultipartDataForClientBuilder data = client.target(baseUri)
                .path("/give-me-file")
                .request(MediaType.MULTIPART_FORM_DATA)
                .get(MultipartDataForClientBuilder.class);
        assertThat(data.file).exists();
        assertThat(data.name).isEqualTo("foo");
        assertThat(data.panda.weight).isEqualTo("huge");
        assertThat(data.panda.height).isEqualTo("medium");
        assertThat(data.panda.mood).isEqualTo("happy");
        assertThat(data.numbers).containsSequence(2008, 2011, 2014);
    }

    void verifyWooHooFile(File file, int expectedTimes) {
        int position = 0;
        try (FileReader reader = new FileReader(file)) {
            int read;
            while ((read = reader.read()) > 0) {
                assertThat((char) read).isEqualTo(WOO_HOO_WOO_HOO_HOO.charAt(position % WOO_HOO_WOO_HOO_HOO.length()));
                position++;
            }
            assertThat(position).isEqualTo(WOO_HOO_WOO_HOO_HOO.length() * expectedTimes);
        } catch (IOException e) {
            fail("failed to read provided file", e);
        }
    }

    @Path("/give-me-file")
    public interface Client {
        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        MultipartData getFile();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/small")
        MultipartData getSmallFile();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/large")
        MultipartData getLargeFile();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/empty")
        MultipartData getFileEmpty();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        MultipartDataWithSetters getFileWithSetters();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/error")
        MultipartData error();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        Uni<MultipartData> uniGetFile();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        CompletionStage<MultipartData> csGetFile();
    }

    @Path("/give-me-file")
    public static class Resource {

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public MultipartData getFile() throws IOException {
            File file = createTempFileToDownload();
            // let's write Woo hoo, woo hoo hoo 10k times
            try (FileOutputStream out = new FileOutputStream(file)) {
                for (int i = 0; i < 10000; i++) {
                    out.write(WOO_HOO_WOO_HOO_HOO.getBytes(StandardCharsets.UTF_8));
                }
            }
            return new MultipartData("foo", file, new Panda("huge", "medium", "happy"),
                    1984, new int[] { 2008, 2011, 2014 });
        }

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/small")
        public MultipartData getSmallFile() throws IOException {
            File file = createTempFileToDownload();
            // let's write Woo hoo, woo hoo hoo 1 time
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(WOO_HOO_WOO_HOO_HOO.getBytes(StandardCharsets.UTF_8));
            }
            return new MultipartData("foo", file, null, 1984, null);
        }

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/large")
        public MultipartData getLargeFile() throws IOException {
            File file = createTempFileToDownload();
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            f.setLength(ONE_GIGA);
            return new MultipartData("foo", file, null, 1984, null);
        }

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/empty")
        public MultipartData getEmptyData() {
            return new MultipartData(null, null, null, 0, null);
        }

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/error")
        public MultipartData throwError() {
            throw new RuntimeException("forced error");
        }

        private static File createTempFileToDownload() throws IOException {
            File file = File.createTempFile("toDownload", ".txt");
            file.deleteOnExit();
            return file;
        }
    }

    public static class MultipartData {

        @RestForm
        @PartType(MediaType.TEXT_PLAIN)
        public String name;

        @RestForm
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;

        @RestForm
        @PartType(MediaType.APPLICATION_JSON)
        public Panda panda;

        @RestForm
        @PartType(MediaType.TEXT_PLAIN)
        public Integer number;

        @RestForm
        @PartType(MediaType.APPLICATION_JSON)
        public int[] numberz;

        public MultipartData() {
        }

        public MultipartData(String name, File file, Panda panda, int number, int[] numberz) {
            this.name = name;
            this.file = file;
            this.panda = panda;
            this.number = number;
            this.numberz = numberz;
        }
    }

    @MultipartForm
    public static class MultipartDataForClientBuilder {
        @RestForm
        @PartType(MediaType.TEXT_PLAIN)
        public String name;

        @RestForm
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;

        @RestForm
        @PartType(MediaType.APPLICATION_JSON)
        public Panda panda;

        @RestForm
        @PartType(MediaType.TEXT_PLAIN)
        public Integer number;

        @FormParam("numberz")
        @PartType(MediaType.APPLICATION_JSON)
        public int[] numbers;

        public MultipartDataForClientBuilder() {
        }

        public MultipartDataForClientBuilder(String name, File file, Panda panda, int number, int[] numberz) {
            this.name = name;
            this.file = file;
            this.panda = panda;
            this.number = number;
            this.numbers = numberz;
        }
    }

    public static class MultipartDataWithSetters {
        @RestForm
        @PartType(MediaType.TEXT_PLAIN)
        private String name;
        @RestForm
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        File file;

        @RestForm
        @PartType(MediaType.APPLICATION_JSON)
        private Panda panda;

        public MultipartDataWithSetters() {
        }

        public MultipartDataWithSetters(String name, File file, Panda panda) {
            this.name = name;
            this.file = file;
            this.panda = panda;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public void setPanda(Panda panda) {
            this.panda = panda;
        }
    }

    public static class Panda {
        public String weight;
        public String height;
        public String mood;

        public Panda() {
        }

        public Panda(String weight, String height, String mood) {
            this.weight = weight;
            this.height = height;
            this.mood = mood;
        }
    }
}
