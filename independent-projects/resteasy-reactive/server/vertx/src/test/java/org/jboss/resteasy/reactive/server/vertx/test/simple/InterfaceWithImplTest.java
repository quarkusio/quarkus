package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InterfaceWithImplTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Greeting.class, GreetingImpl.class));

    @Test
    public void test() {
        RestAssured.get("/hello/greeting/universe")
                .then().body(Matchers.equalTo("name: universe / blocking: true"));

        RestAssured.get("/hello/greeting2/universe")
                .then().body(Matchers.equalTo("name: universe / blocking: false"));
    }

    @Path("/hello")
    @NonBlocking
    public interface Greeting {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("/greeting/{name}")
        String greeting(String name);

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("/greeting2/{name}")
        String greeting2(String name);
    }

    public static class GreetingImpl implements Greeting {

        @Override
        @Blocking
        public String greeting(String name) {
            return resultString(name);
        }

        @Override
        public String greeting2(String name) {
            return resultString(name);
        }

        private String resultString(String name) {
            return "name: " + name + " / blocking: " + BlockingOperationSupport.isBlockingAllowed();
        }
    }

}
