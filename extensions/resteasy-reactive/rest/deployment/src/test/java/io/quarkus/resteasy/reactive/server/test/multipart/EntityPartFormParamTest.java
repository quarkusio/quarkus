package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class EntityPartFormParamTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            });

    @Test
    public void formParamEntityPart() {
        String response = given()
                .multiPart("name", "Alice")
                .accept("text/plain")
                .when()
                .post("/entity-part-form/jaxrs")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isEqualTo("name=Alice");
    }

    @Test
    public void restFormEntityPart() {
        String response = given()
                .multiPart("name", "Bob")
                .accept("text/plain")
                .when()
                .post("/entity-part-form/quarkus")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isEqualTo("name=Bob");
    }

    @Test
    public void mixedParams() {
        String response = given()
                .multiPart("name", "Charlie")
                .multiPart("age", "25")
                .accept("text/plain")
                .when()
                .post("/entity-part-form/mixed")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isEqualTo("name=Charlie,age=25");
    }

    @Path("/entity-part-form")
    public static class Resource {

        @POST
        @Path("jaxrs")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String jaxrs(@FormParam("name") EntityPart namePart) throws IOException {
            String content = new String(namePart.getContent().readAllBytes(), StandardCharsets.UTF_8);
            return namePart.getName() + "=" + content;
        }

        @POST
        @Path("quarkus")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String quarkus(@RestForm EntityPart name) throws IOException {
            String content = new String(name.getContent().readAllBytes(), StandardCharsets.UTF_8);
            return name.getName() + "=" + content;
        }

        @POST
        @Path("mixed")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String mixed(@FormParam("name") EntityPart namePart, @FormParam("age") String age) throws IOException {
            String content = new String(namePart.getContent().readAllBytes(), StandardCharsets.UTF_8);
            return namePart.getName() + "=" + content + ",age=" + age;
        }
    }
}
