package io.quarkus.it.rest.client.multipart;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.http.HttpServerOptions;

@Disabled
@QuarkusTest
public class LargerThanDefaultFormAttributeMultipartFormInputTest {
    private final File FILE = new File("./src/main/resources/larger-than-default-form-attribute.txt");

    @Test
    public void test() throws IOException {
        String fileContents = new String(Files.readAllBytes(FILE.toPath()), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append(fileContents);
        }
        fileContents = sb.toString();

        Assertions.assertTrue(fileContents.length() > HttpServerOptions.DEFAULT_MAX_FORM_ATTRIBUTE_SIZE);
        given()
                .multiPart("text", fileContents)
                .accept("text/plain")
                .when()
                .post("/file")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo(fileContents));
    }

}
