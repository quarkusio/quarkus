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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class FileInputWithoutDeleteTest extends AbstractMultipartTest {

    private static final java.nio.file.Path uploadDir = Paths.get("file-uploads");

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class)
                            .addAsResource(new StringAsset(
                                    // keep the files around so we can assert the outcome
                                    "quarkus.http.body.delete-uploaded-files-on-end=false\nquarkus.http.body.uploads-directory="
                                            + uploadDir.toString() + "\n"),
                                    "application.properties");
                }

            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");
    private final File HTML_FILE2 = new File("./src/test/resources/test2.html");

    @BeforeEach
    public void assertEmptyUploads() {
        Assertions.assertTrue(isDirectoryEmpty(uploadDir));
    }

    @AfterEach
    public void clearDirectory() {
        clearDirectory(uploadDir);
    }

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

        // ensure that the 3 uploaded files where created on disk
        Assertions.assertEquals(1, uploadDir.toFile().listFiles().length);

        RestAssured.given()
                .contentType("application/octet-stream")
                .body(HTML_FILE2)
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(equalTo(fileSizeAsStr(HTML_FILE2)));

        // ensure that the 3 uploaded files where created on disk
        Assertions.assertEquals(2, uploadDir.toFile().listFiles().length);
    }

    @Path("test")
    public static class Resource {

        @POST
        @Consumes("application/octet-stream")
        public long size(File file) throws IOException {
            return Files.size(file.toPath());
        }
    }
}
