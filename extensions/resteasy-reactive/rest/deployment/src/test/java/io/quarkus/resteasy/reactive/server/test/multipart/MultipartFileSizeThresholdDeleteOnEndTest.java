package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.response.Response;

/**
 * A part kept in memory that is written to the uploads directory on demand is deleted at the end of the request
 * like any other uploaded file.
 */
public class MultipartFileSizeThresholdDeleteOnEndTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MultipartFileSizeThresholdTest.Resource.class,
                    MultipartFileSizeThresholdTest.Form.class))
            .overrideRuntimeConfigKey("quarkus.http.body.multipart.file-size-threshold", "10K")
            .overrideRuntimeConfigKey("quarkus.http.body.uploads-directory", "target/multipart-threshold-uploads-deleted");

    @Test
    void lazilyWrittenFileIsDeletedAtTheEndOfTheRequest() {
        Response response = given()
                .multiPart("small", "small.bin", new byte[1024], MediaType.APPLICATION_OCTET_STREAM)
                .post("/multipart/uploads");
        response.then().statusCode(200);
        Path path = Paths.get(response.asString());
        assertThat(path.toAbsolutePath().getParent())
                .isEqualTo(Paths.get("target/multipart-threshold-uploads-deleted").toAbsolutePath());
        await().untilAsserted(() -> assertThat(path).doesNotExist());
    }
}
