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
                            .addClasses(Resource.class, FormDataWithEntityPart.class);
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

    @Test
    public void entityPartReaderWriterRoundTrip() {
        String response = given()
                .multiPart("value", "42")
                .accept("text/plain")
                .when()
                .post("/entity-part-form/provider-roundtrip")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isEqualTo("42");
    }

    @Test
    public void entityPartInContainer() {
        String response = given()
                .multiPart("file", "test.txt", "file content".getBytes(), "application/octet-stream")
                .multiPart("name", "Alice")
                .accept("text/plain")
                .when()
                .post("/entity-part-form/container")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isEqualTo("file=file content,name=Alice");
    }

    public static class FormDataWithEntityPart {
        @RestForm
        public EntityPart file;

        @RestForm
        public String name;
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

        @POST
        @Path("provider-roundtrip")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String providerRoundTrip(@RestForm EntityPart value) throws IOException {
            // Reader path: deserialize text/plain content as Integer via MessageBodyReader
            Integer num = value.getContent(Integer.class);
            // Writer path: serialize Integer via MessageBodyWriter in EntityPart.Builder
            EntityPart built = EntityPart.withName("result")
                    .content(num, Integer.class)
                    .mediaType(MediaType.TEXT_PLAIN_TYPE)
                    .build();
            // Read back the writer's output to verify it serialized correctly
            return built.getContent(String.class);
        }

        @POST
        @Path("container")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String container(FormDataWithEntityPart form) throws IOException {
            String fileContent = new String(form.file.getContent().readAllBytes(), StandardCharsets.UTF_8);
            return "file=" + fileContent + ",name=" + form.name;
        }
    }
}
