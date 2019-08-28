package io.quarkus.it.tika;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TikaEmbeddedContentTest {

    @Test
    public void testGetOuterText() throws Exception {
        given()
                .when().header("Content-Type", "application/vnd.ms-excel")
                .body(readTestFile("testEXCEL_embeded.xls"))
                .post("/embedded/outerText")
                .then()
                .statusCode(200)
                .body(containsString("Sheet1"));
    }

    @Test
    public void testGetInnerText() throws Exception {
        given()
                .when().header("Content-Type", "application/vnd.ms-excel")
                .body(readTestFile("testEXCEL_embeded.xls"))
                .post("/embedded/innerText")
                .then()
                .statusCode(200)
                .body(containsString("The quick brown fox jumps over the lazy dog"));
    }

    private byte[] readTestFile(String fileName) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return readBytes(is);
        }
    }

    static byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

}
