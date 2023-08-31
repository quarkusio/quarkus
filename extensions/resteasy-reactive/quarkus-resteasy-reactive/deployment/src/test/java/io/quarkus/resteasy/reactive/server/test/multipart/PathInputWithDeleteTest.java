package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PathInputWithDeleteTest extends AbstractMultipartTest {

    private static final java.nio.file.Path uploadDir = Paths.get("file-uploads");

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.http.body.uploads-directory="
                                            + uploadDir.toString() + "\n"),
                                    "application.properties");
                }

            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");
    private final File HTML_FILE2 = new File("./src/test/resources/test2.html");

    @Test
    public void test() throws IOException {
        RestAssured.given()
                .contentType("application/octet-stream")
                .body(HTML_FILE)
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(equalTo(fileSizeAsStr(HTML_FILE)));

        awaitUploadDirectoryToEmpty(uploadDir);

        RestAssured.given()
                .contentType("application/octet-stream")
                .body(HTML_FILE2)
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(equalTo(fileSizeAsStr(HTML_FILE2)));

        awaitUploadDirectoryToEmpty(uploadDir);
    }

    @Path("test")
    public static class Resource {

        @POST
        @Consumes("application/octet-stream")
        public long size(java.nio.file.Path file) throws IOException {
            return Files.size(file);
        }
    }
}
