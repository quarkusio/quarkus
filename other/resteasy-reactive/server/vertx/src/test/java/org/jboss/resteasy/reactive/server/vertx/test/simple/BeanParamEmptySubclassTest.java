package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.restassured.RestAssured;
import javax.ws.rs.BeanParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanParamEmptySubclassTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, BaseParams.class, Params.class));

    @Test
    public void test() {
        RestAssured.given().formParam("param1", "foo").post("/bean?param2=bar")
                .then().statusCode(200).body(Matchers.equalTo("foo/bar"));
    }

    @Path("bean")
    public static class TestResource {

        @POST
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(final @BeanParam BaseParams params) {
            return params.getParam1() + "/" + params.getParam2();
        }
    }

    public static class BaseParams {
        @FormParam("param1")
        private String param1;

        @QueryParam("param2")
        private String param2;

        public String getParam1() {
            return param1;
        }

        public String getParam2() {
            return param2;
        }
    }

    public static class Params extends BaseParams {

    }
}
