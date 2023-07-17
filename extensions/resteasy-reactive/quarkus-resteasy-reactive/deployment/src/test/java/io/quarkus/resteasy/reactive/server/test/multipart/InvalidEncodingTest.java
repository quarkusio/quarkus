package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.hamcrest.CoreMatchers.not;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
                    .addClasses(FeedbackBody.class, FeedbackResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.resteasy-reactive.multipart.input-part.default-charset=us-ascii"),
                            "application.properties"));

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

    @Path("/test")
    public static class FeedbackResource {

        @POST
        @Path("/multipart-encoding")
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.MULTIPART_FORM_DATA + ";charset=UTF-8")
        public String postForm(@BeanParam final FeedbackBody feedback) {
            return feedback.content;
        }
    }

    public static class FeedbackBody {
        @RestForm("content")
        public String content;
    }
}
