package io.quarkus.resteasy.multipart;

import static org.hamcrest.CoreMatchers.not;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.specification.MultiPartSpecification;

public class InvalidEncodingTest {

    private static final String TEXT_WITH_ACCENTED_CHARACTERS = "Text with UTF-8 accented characters: é à è";

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FeedbackBody.class, FeedbackResource.class))
            .withConfigurationResource("application-charset-us-ascii.properties");

    @Test
    public void testMultipartEncoding() throws URISyntaxException {
        MultiPartSpecification multiPartSpecification = new MultiPartSpecBuilder(TEXT_WITH_ACCENTED_CHARACTERS)
                .controlName("content")
                // we need to force the content-type to avoid having the charset included
                // as we are testing the default behavior when no charset is defined
                .header("Content-Type", "text/plain")
                .charset(StandardCharsets.UTF_8)
                .build();

        RestAssured
                .given()
                .multiPart(multiPartSpecification)
                .post("/test/multipart-encoding")
                .then()
                .statusCode(200)
                .body(not(TEXT_WITH_ACCENTED_CHARACTERS));
    }

}
