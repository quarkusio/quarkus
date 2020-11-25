package io.quarkus.rest.server.test.simple;

import java.util.function.Supplier;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ApplicationPathTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestApplication.class, HelloResource.class);
                }
            });

    @Test
    public void helloTest() {
        RestAssured.get("/app/hello")
                .then().body(Matchers.equalTo("hello"));
    }

    @ApplicationPath("app")
    public static class TestApplication extends Application {

    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello";
        }
    }

}
