package io.quarkus.resteasy.reactive.server.test.simple;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ContextParamFromCdiTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ContextFromCdi.class, ContextFromCdiResource.class));

    @Test
    public void testParam() {
        RestAssured.get("/context-from-cdi")
                .then().statusCode(200).body(Matchers.equalTo("context"));
    }

    @ApplicationScoped
    @Unremovable
    public static class ContextFromCdi {

        public String get() {
            return "context";
        }
    }

    @Path("context-from-cdi")
    public static class ContextFromCdiResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get(@Context ContextFromCdi cdi) {
            return cdi.get();
        }
    }

}
