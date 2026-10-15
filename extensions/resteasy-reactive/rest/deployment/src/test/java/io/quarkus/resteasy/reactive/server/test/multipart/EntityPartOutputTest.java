package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.response.ExtractableResponse;

public class EntityPartOutputTest {

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
    public void entityPartListReturn() {
        ExtractableResponse<?> response = given()
                .accept("multipart/form-data")
                .when()
                .get("/entity-part-output/list")
                .then()
                .statusCode(200)
                .extract();

        String contentType = response.contentType();
        assertThat(contentType).startsWith("multipart/form-data");
        assertThat(contentType).contains("boundary");

        String body = response.body().asString();
        assertThat(body).contains("greeting");
        assertThat(body).contains("hello world");
        assertThat(body).contains("count");
        assertThat(body).contains("42");
    }

    @Test
    public void responseWrappedEntityParts() {
        ExtractableResponse<?> response = given()
                .accept("multipart/form-data")
                .when()
                .get("/entity-part-output/response")
                .then()
                .statusCode(200)
                .extract();

        String contentType = response.contentType();
        assertThat(contentType).startsWith("multipart/form-data");

        String body = response.body().asString();
        assertThat(body).contains("message");
        assertThat(body).contains("from response");
    }

    @Path("/entity-part-output")
    public static class Resource {

        @GET
        @Path("list")
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public List<EntityPart> getList() throws IOException {
            return List.of(
                    EntityPart.withName("greeting")
                            .content("hello world")
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .build(),
                    EntityPart.withName("count")
                            .content("42")
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .build());
        }

        @GET
        @Path("response")
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public Response getResponse() throws IOException {
            List<EntityPart> parts = List.of(
                    EntityPart.withName("message")
                            .content("from response")
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .build());
            return Response.ok(parts).build();
        }
    }
}
