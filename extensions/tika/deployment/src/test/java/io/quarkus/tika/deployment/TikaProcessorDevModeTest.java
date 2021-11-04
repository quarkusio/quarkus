package io.quarkus.tika.deployment;

import static org.hamcrest.CoreMatchers.is;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class TikaProcessorDevModeTest {

    private static Class<?>[] testClasses = {
            TikaParserResource.class
    };

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("tika-config.xml")
                    .addAsResource("application-dev-mode.properties", "application.properties"));

    @Test
    public void testPdf() throws Exception {
        RestAssured.given()
                .when().header("Content-Type", "application/pdf")
                .body(readQuarkusFile("quarkus.pdf"))
                .post("/parse/text")
                .then()
                .statusCode(200)
                .body(is("Hello Quarkus"));
    }

    private byte[] readQuarkusFile(String fileName) throws Exception {
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
