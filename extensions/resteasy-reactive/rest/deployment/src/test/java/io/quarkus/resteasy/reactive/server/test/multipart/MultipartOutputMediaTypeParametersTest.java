package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * The parameters of the produced multipart media type are kept when the boundary is added to it.
 */
public class MultipartOutputMediaTypeParametersTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @Test
    public void parametersOfTheProducedMediaTypeAreKept() {
        RestAssured.get("/multipart/media-type-parameters")
                .then()
                .statusCode(200)
                .header("Content-Type", startsWith("multipart/form-data"))
                .header("Content-Type", containsString("charset=UTF-8"))
                .header("Content-Type", containsString("boundary="));
    }

    @Path("/multipart")
    public static class Resource {

        @GET
        @Path("/media-type-parameters")
        @Produces(MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8")
        public MultipartFormDataOutput withCharset() {
            MultipartFormDataOutput output = new MultipartFormDataOutput();
            output.addFormData("name", "value", MediaType.TEXT_PLAIN_TYPE);
            return output;
        }
    }
}
