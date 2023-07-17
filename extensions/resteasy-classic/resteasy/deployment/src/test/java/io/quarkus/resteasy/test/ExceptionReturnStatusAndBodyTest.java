package io.quarkus.resteasy.test;

import static org.hamcrest.Matchers.containsString;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ExceptionReturnStatusAndBodyTest {

    private static final String RESOURCE_PATH = "/exception-return-status-and-body";

    private static final String EXCEPTION_MESSAGE = "RuntimeException: Forced exception!";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExceptionReturnStatusAndBodyResource.class));

    @Test
    public void testDelete() {
        RestAssured.when().delete(RESOURCE_PATH).then().statusCode(500).body(containsString(EXCEPTION_MESSAGE));
    }

    @Test
    public void testGet() {
        RestAssured.when().get(RESOURCE_PATH).then().statusCode(500).body(containsString(EXCEPTION_MESSAGE));
    }

    @Test
    public void testHead() {
        RestAssured.when().head(RESOURCE_PATH).then().statusCode(500);
    }

    @Test
    public void testOptions() {
        RestAssured.when().options(RESOURCE_PATH).then().statusCode(500).body(containsString(EXCEPTION_MESSAGE));
    }

    @Test
    public void testPatch() {
        RestAssured.when().patch(RESOURCE_PATH).then().statusCode(500).body(containsString(EXCEPTION_MESSAGE));
    }

    @Test
    public void testPost() {
        RestAssured.when().post(RESOURCE_PATH).then().statusCode(500).body(containsString(EXCEPTION_MESSAGE));
    }

    @Test
    public void testPut() {
        RestAssured.when().put(RESOURCE_PATH).then().statusCode(500).body(containsString(EXCEPTION_MESSAGE));
    }

    @Path(RESOURCE_PATH)
    public static class ExceptionReturnStatusAndBodyResource {

        @DELETE
        public void deleteException() {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }

        @GET
        public void getException() {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }

        @HEAD
        public void headException() {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }

        @OPTIONS
        public void optionsException() {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }

        @PATCH
        public void patchException() {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }

        @POST
        public void postException() {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }

        @PUT
        public void putException() {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }
    }
}
