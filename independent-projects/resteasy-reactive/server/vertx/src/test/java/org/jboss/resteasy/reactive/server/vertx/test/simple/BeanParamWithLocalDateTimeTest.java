package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.restassured.RestAssured;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanParamWithLocalDateTimeTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloResource.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/hello?date=2007-12-03T10:15:30")
                .then().statusCode(200).body(Matchers.equalTo("hello#2007"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@BeanParam Container container) {
            return "hello#" + container.getDate().getYear();
        }
    }

    public static class Container {
        @QueryParam("date")
        private LocalDateTime date;

        public LocalDateTime getDate() {
            return date;
        }

        public void setDate(LocalDateTime date) {
            this.date = date;
        }
    }

}
