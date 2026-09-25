package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class EntityPartCharsetTest {

    private static final String BOUNDARY = "XyZ";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class));

    @Test
    void fieldPartReadAsEntityPart() {
        assertThat(post("/charset/entity-part", field())).isEqualTo("café");
    }

    @Test
    void fieldPartReadAsString() {
        assertThat(post("/charset/string", field())).isEqualTo("café");
    }

    @Test
    void filePartReadAsEntityPart() {
        assertThat(post("/charset/entity-part", file())).isEqualTo("café");
    }

    @Test
    void filePartReadAsString() {
        assertThat(post("/charset/string", file())).isEqualTo("café");
    }

    @Test
    void partWithoutCharsetIsReadAsUtf8() {
        byte[] body = part("Content-Disposition: form-data; name=\"text\"\r\nContent-Type: text/plain\r\n",
                "café".getBytes(StandardCharsets.UTF_8));
        assertThat(post("/charset/entity-part", body)).isEqualTo("café");
    }

    private static String post(String path, byte[] body) {
        return given()
                .contentType("multipart/form-data; boundary=" + BOUNDARY)
                .body(body)
                .when().post(path)
                .then().statusCode(200)
                .extract().asString();
    }

    private static byte[] field() {
        return part("Content-Disposition: form-data; name=\"text\"\r\nContent-Type: text/plain; charset=ISO-8859-1\r\n",
                "café".getBytes(StandardCharsets.ISO_8859_1));
    }

    private static byte[] file() {
        return part(
                "Content-Disposition: form-data; name=\"text\"; filename=\"t.txt\"\r\nContent-Type: text/plain; charset=ISO-8859-1\r\n",
                "café".getBytes(StandardCharsets.ISO_8859_1));
    }

    private static byte[] part(String headers, byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(("--" + BOUNDARY + "\r\n" + headers + "\r\n").getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(content);
        out.writeBytes(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }

    @Path("/charset")
    public static class Resource {

        @POST
        @Path("entity-part")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN + ";charset=UTF-8")
        public String entityPart(@FormParam("text") EntityPart part) throws IOException {
            return part.getContent(String.class);
        }

        @POST
        @Path("string")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN + ";charset=UTF-8")
        public String string(@FormParam("text") String text) {
            return text;
        }
    }
}
