package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class EntityPartInputTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");

    @Test
    public void singleTextField() {
        String response = given()
                .multiPart("greeting", "hello")
                .accept("text/plain")
                .when()
                .post("/entity-part")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isEqualTo("1:greeting=hello");
    }

    @Test
    public void multipleFields() {
        String response = given()
                .multiPart("name", "Alice")
                .multiPart("age", "30")
                .accept("text/plain")
                .when()
                .post("/entity-part")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).contains("2:");
        assertThat(response).contains("name=Alice");
        assertThat(response).contains("age=30");
    }

    @Test
    public void fileUpload() {
        String response = given()
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .accept("text/plain")
                .when()
                .post("/entity-part")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).startsWith("1:");
        assertThat(response).contains("htmlFile");
        assertThat(response).contains("filename=test.html");
    }

    @Path("/entity-part")
    public static class Resource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String receive(List<EntityPart> parts) throws IOException {
            List<String> descriptions = new ArrayList<>();
            for (EntityPart part : parts) {
                StringBuilder sb = new StringBuilder();
                sb.append(part.getName());
                String content = new String(part.getContent().readAllBytes(), StandardCharsets.UTF_8);
                sb.append("=").append(content);
                part.getFileName().ifPresent(f -> sb.append(",filename=").append(f));
                descriptions.add(sb.toString());
            }
            return parts.size() + ":" + String.join(";", descriptions);
        }
    }
}
