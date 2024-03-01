package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipartTextWithoutFilenameTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            });

    @Test
    public void test() throws IOException {
        given()
                .contentType("multipart/form-data")
                .multiPart("firstParam", "{\"id\":\"myId\",\"name\":\"myName\"}", "application/json")
                .when()
                .post("/test")
                .then()
                .statusCode(200);
    }

    @Path("/test")
    public static class Resource {

        @POST
        public RestResponse<Void> testMultipart(@FormParam("firstParam") final String firstParam,
                @FormParam("secondParam") final String secondParam) {
            return RestResponse.ok();
        }
    }
}
