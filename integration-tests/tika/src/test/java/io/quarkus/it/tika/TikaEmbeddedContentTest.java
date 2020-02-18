package io.quarkus.it.tika;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TikaEmbeddedContentTest {

    @Inject
    @ConfigProperty(name = "quarkus.tika.parser-file-format-support.ooxml.pptx")
    Optional<Boolean> parsePptx;
    @Inject
    @ConfigProperty(name = "quarkus.tika.parser-file-format-support.ooxml.xlsx")
    Optional<Boolean> parseXlsx;

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

    @Test
    public void contentTypePPTXText() throws Exception {
        if (true || !parsePptx.orElse(true)) {
            return;
        }
        contentTypeText("testPPTX_embedded.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    @Test
    public void contentTypeXLSXText() throws Exception {
        if (true || !parseXlsx.orElse(true)) {
            return;
        }
        contentTypeText("testXLSX_embedded.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    public void contentTypeDOCXText() throws Exception {
        contentTypeText("testDOCX_embedded.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    public void contentTypeEXCELText() throws Exception {
        contentTypeText("testEXCEL_embeded.xls", "application/vnd.ms-excel");
    }

    private void contentTypeText(String fileName, String expected) throws Exception {
        given()
                .when()
                .body(readTestFile(fileName))
                .post("/embedded/contentType")
                .then()
                .statusCode(200)
                .body(containsString(expected));
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
