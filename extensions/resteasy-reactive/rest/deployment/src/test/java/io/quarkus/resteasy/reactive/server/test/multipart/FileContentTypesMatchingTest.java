package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.hamcrest.Matchers.equalTo;

import java.nio.charset.StandardCharsets;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * A part without a file name is a file part when its media type is listed in
 * {@code quarkus.http.body.multipart.file-content-types}, whatever the case of the header and its parameters.
 */
public class FileContentTypesMatchingTest {

    private static final String BOUNDARY = "----file-content-types-boundary";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FileContentTypesMatchingResource.class)
                    .addAsResource(new StringAsset("quarkus.http.body.multipart.file-content-types=text/xml\n"),
                            "application.properties"));

    @Test
    public void listedContentTypeIsMatchedWithParametersAndIgnoringCase() {
        String body = part("exact", "text/xml")
                + part("withParameter", "text/xml; charset=UTF-8")
                + part("upperCase", "Text/XML")
                + part("other", "text/plain")
                + "--" + BOUNDARY + "--\r\n";

        RestAssured.given()
                .contentType("multipart/form-data; boundary=" + BOUNDARY)
                .body(body.getBytes(StandardCharsets.UTF_8))
                .accept("text/plain")
                .when()
                .post("/file-content-types")
                .then()
                .statusCode(200)
                .body(equalTo("exact,upperCase,withParameter"));
    }

    private static String part(String name, String contentType) {
        return "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "\r\n"
                + name + " content\r\n";
    }
}
