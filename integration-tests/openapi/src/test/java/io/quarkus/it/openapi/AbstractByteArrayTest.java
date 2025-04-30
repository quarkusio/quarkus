package io.quarkus.it.openapi;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import io.restassured.RestAssured;

public abstract class AbstractByteArrayTest extends AbstractTest {
    protected static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    protected void testServiceByteArrayRequest(String path, String expectedContentType) throws IOException {

        byte[] b = Files.readAllBytes(tempFile().toPath());
        byte[] responseFile = RestAssured
                .with().body(b)
                .and()
                .with().contentType(APPLICATION_OCTET_STREAM)
                .when()
                .post(path)
                .then()
                .header("Content-Type", Matchers.startsWith(APPLICATION_OCTET_STREAM))
                .extract().asByteArray();

        Assertions.assertEquals(b.length, responseFile.length);

    }

    protected void testServiceByteArrayResponse(String path, String expectedResponseType)
            throws UnsupportedEncodingException, IOException {
        // Service
        File f = tempFile();
        byte[] b = Files.readAllBytes(f.toPath());
        String filename = URLEncoder.encode(f.getAbsoluteFile().toString(), "UTF-8");
        byte[] responseFile = RestAssured
                .when()
                .get(path + "/" + filename)
                .then()
                .header("Content-Type", Matchers.startsWith(expectedResponseType))
                .and()
                .extract().asByteArray();

        Assertions.assertEquals(b.length, responseFile.length);
    }

    private File tempFile() {
        try {
            java.nio.file.Path createTempFile = Files.createTempFile("", "");
            return createTempFile.toFile();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
