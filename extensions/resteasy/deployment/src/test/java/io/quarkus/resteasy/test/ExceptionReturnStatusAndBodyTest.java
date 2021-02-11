package io.quarkus.resteasy.test;

import static org.hamcrest.Matchers.containsString;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ExceptionReturnStatusAndBodyTest {

    private static final String RESOURCE_PATH = "/exception-return-status-and-body";

    private static final String EXCEPTION_MESSAGE = "RuntimeException: Forced exception!";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
