package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MultipartResponseTest {

    public static final String WOO_HOO_WOO_HOO_HOO = "Woo hoo, woo hoo hoo";
    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Test
    void shouldParseMultipartResponse() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultipartData data = client.getFile();
        assertThat(data.file).exists();
        verifyWooHooFile(data.file);
        assertThat(data.name).isEqualTo("foo");
        assertThat(data.panda.weight).isEqualTo("huge");
        assertThat(data.panda.height).isEqualTo("medium");
        assertThat(data.panda.mood).isEqualTo("happy");
        assertThat(data.number).isEqualTo(1984);
        assertThat(data.numberz).containsSequence(2008, 2011, 2014);
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
        javax.ws.rs.client.Client client = ClientBuilder.newBuilder().build();
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

    void verifyWooHooFile(File file) {
        int position = 0;
        try (FileReader reader = new FileReader(file)) {
            int read;
            while ((read = reader.read()) > 0) {
                assertThat((char) read).isEqualTo(WOO_HOO_WOO_HOO_HOO.charAt(position % WOO_HOO_WOO_HOO_HOO.length()));
                position++;
            }
            assertThat(position).isEqualTo(WOO_HOO_WOO_HOO_HOO.length() * 10000);
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
        @Path("/empty")
        MultipartData getFileEmpty();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        MultipartDataWithSetters getFileWithSetters();

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/error")
        MultipartData error();
    }

    @Path("/give-me-file")
    public static class Resource {

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public MultipartData getFile() throws IOException {
            File file = File.createTempFile("toDownload", ".txt");
            file.deleteOnExit();
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
