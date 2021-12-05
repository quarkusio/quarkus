package io.quarkus.resteasy.test.root;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test a combination of application path and http root path.
 */
public class ApplicationPathHttpRootTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, HelloApp.class, BaseApplication.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/foo"), "application.properties"));

    @Test
    public void testResources() {
        // Note that /foo is added automatically by RestAssuredURLManager 
        RestAssured.when().get("/hello/world").then().body(Matchers.is("hello world"));
        RestAssured.when().get("/world").then().statusCode(404);
    }

    @Path("world")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello world";
        }

    }

    @ApplicationPath("hello")
    public static class HelloApp extends BaseApplication {

    }

    public static abstract class BaseApplication extends Application {

    }

}
