package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipartBinaryWithoutFilenameTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(MultipartDataInputTest.Resource.class, MultipartDataInputTest.Item.class,
                                    MultipartDataInputTest.Result.class);
                }
            });
    private final File IMAGE_FILE = new File("./src/test/resources/image.png");

    @Test
    public void test() throws IOException {
        byte[] bytes = given()
                .contentType("multipart/form-data")
                .multiPart("bytes", IMAGE_FILE, "application/png")
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .extract().body().asByteArray();

        assertThat(bytes).isEqualTo(Files.readAllBytes(IMAGE_FILE.toPath()));
    }

    @Path("/test")
    public static class Resource {

        @POST
        public byte[] testMultipart(Input input) {
            return input.bytes;
        }
    }

    public static class Input {
        @RestForm("bytes")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] bytes;

    }
}
