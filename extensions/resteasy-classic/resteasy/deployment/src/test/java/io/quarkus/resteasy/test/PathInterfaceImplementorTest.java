package io.quarkus.resteasy.test;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PathInterfaceImplementorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AlphaResource.class, AlphaResourceImpl.class, TestService.class));

    @Test
    public void testConstructorInjectionResource() {
        RestAssured.when().get("/alpha").then().body(Matchers.is("pong"));
        RestAssured.when().get("/bravo").then().body(Matchers.is("pong"));
    }

    @Path("alpha")
    public interface AlphaResource {

        @GET
        String get();

    }

    // No annotation - @Singleton is used by default
    public static class AlphaResourceImpl implements AlphaResource {

        @Inject
        TestService service;

        public String get() {
            return service.ping();
        }

    }

    @Path("bravo")
    public interface BravoResource {

        @GET
        String get();

    }

    @RequestScoped
    public static class BravoResourceImpl implements BravoResource {

        @Inject
        TestService service;

        public String get() {
            return service.ping();
        }

    }

    @Singleton
    public static class TestService {

        String ping() {
            return "pong";
        }
    }

}
