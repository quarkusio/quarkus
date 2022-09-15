package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.restassured.RestAssured;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.time.Duration;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InstantParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class));

    @Test
    public void test() {
        RestAssured.get("/hello?instant=1984-08-08T01:02:03Z")
                .then().statusCode(200).body(Matchers.equalTo("hello#1984-08-09T01:02:03Z"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@RestQuery Instant instant) {
            return "hello#" + instant.plus(Duration.ofDays(1)).toString();
        }
    }

}
