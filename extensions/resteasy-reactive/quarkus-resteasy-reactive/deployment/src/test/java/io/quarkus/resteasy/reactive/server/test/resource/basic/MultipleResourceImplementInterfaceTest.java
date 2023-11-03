package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleResourceImplementInterfaceTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(HelloResource.class);
                    war.addClass(Hello2Resource.class);
                    war.addClass(Shared.class);
                    return war;
                }
            });

    @Test
    public void hello() {
        when().get("/hello")
                .then().statusCode(200).body(is("hello"));

        when().get("/hello/shared/1")
                .then().statusCode(200).body(is("1"));
    }

    @Test
    public void hello2() {
        when().get("/hello2")
                .then().statusCode(200).body(is("hello2"));

        when().get("/hello2/shared/h2")
                .then().statusCode(200).body(is("h2"));
    }

    @Path("/hello")
    public static class HelloResource implements Shared {

        @GET
        public String hello() {
            return "hello";
        }

    }

    @Path("/hello2")
    public static class Hello2Resource implements Shared {

        @GET
        public String hello() {
            return "hello2";
        }

    }

    public interface Shared {

        @GET
        @Path("/shared/{id}")
        default String version(@RestPath String id) {
            return id;
        }
    }
}
