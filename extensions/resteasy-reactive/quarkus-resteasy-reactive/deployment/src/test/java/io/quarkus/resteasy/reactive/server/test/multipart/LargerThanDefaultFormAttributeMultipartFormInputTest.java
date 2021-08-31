package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import io.vertx.core.http.HttpServerOptions;

public class LargerThanDefaultFormAttributeMultipartFormInputTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, Data.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.http.limits.max-form-attribute-size=120K"),
                                    "application.properties");
                }
            });

    private final File FILE = new File("./src/test/resources/larger-than-default-form-attribute.txt");

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
                .post("/test")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo(fileContents));
    }

    @Path("/test")
    public static class Resource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@MultipartForm Data data) {
            return data.getText();
        }
    }

    public static class Data {
        @FormParam("text")
        @PartType("text/plain")
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

}
