package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MultipartInputStreamResponseTest {

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(
                    (jar) -> jar.addClasses(Resource.class, Client.class, SimpleMultipartInput.class));

    @Test
    void shouldParseMultipartResponse() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        InputStream data = client.getFile();
        try (SimpleMultipartInput multipart = new SimpleMultipartInput(Resource.MEDIA_TYPE)) {
            multipart.parse(data);

            Map<String, List<SimpleMultipartInput.Part>> formData = multipart.getFormDataMap();
            assertThat(formData).containsKeys("part1", "part2", "data.txt");
            assertThat(formData.get("part1").get(0).getBodyAsString()).isEqualTo("This is Value 1");
            assertThat(formData.get("part2").get(0).getBodyAsString()).isEqualTo("This is Value 2");
            assertThat(formData.get("data.txt").get(0).getBodyAsString()).isEqualTo("hello world");
            assertThat(formData.get("data.txt").get(0).getFileName()).isEqualTo("data.txt");
        }
    }

    @Path("/multipart")
    public interface Client {
        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        InputStream getFile();
    }

    @Path("/multipart")
    public static class Resource {

        private static final String DATA = "URLSTR: file:/data.txt\r\n"
                + "--B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3\r\n"
                + "Content-Disposition: form-data; name=\"part1\"\r\n"
                + "Content-Type: text/plain; charset=US-ASCII\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + "This is Value 1\r\n"
                + "--B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3\r\n"
                + "Content-Disposition: form-data; name=\"part2\"\r\n"
                + "Content-Type: text/plain; charset=US-ASCII\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + "This is Value 2\r\n"
                + "--B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3\r\n"
                + "Content-Disposition: form-data; name=\"data.txt\"; filename=\"data.txt\"\r\n"
                + "Content-Type: application/octet-stream; charset=ISO-8859-1\r\n"
                + "Content-Transfer-Encoding: binary\r\n"
                + "\r\n"
                + "hello world\r\n" + "--B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3--";
        public static final MediaType MEDIA_TYPE = new MediaType("multipart", "form-data",
                Map.of("boundary", "B98hgCmKsQ-B5AUFnm2FnDRCgHPDE3"));

        @GET
        public Response getFile() {
            return Response.ok(DATA,
                    MEDIA_TYPE).build();
        }

    }

}
