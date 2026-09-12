package io.quarkus.resteasy.reactive.server.test.beanparam;

import static org.hamcrest.CoreMatchers.is;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class GenericInterfaceBeanParamTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(PageFilter.class, UserFilter.class,
                    PageResource.class, UserResource.class));

    @Test
    void shouldBindFieldsDeclaredInTheResolvedBeanParamType() {
        RestAssured.given()
                .queryParam("number", "3")
                .queryParam("size", "7")
                .queryParam("username", "bob")
                .queryParam("fullName", "Bob Smith")
                .get("/api/user/list")
                .then()
                .statusCode(200)
                .body(is("number=3,size=7,username=bob,fullName=Bob Smith"));
    }

    @Test
    void shouldApplyDefaultValuesDeclaredInTheBaseType() {
        RestAssured.given()
                .queryParam("username", "bob")
                .get("/api/user/list")
                .then()
                .statusCode(200)
                .body(is("number=0,size=20,username=bob,fullName=null"));
    }

    public abstract static class PageFilter {

        @RestQuery
        @DefaultValue("0")
        protected Integer number;

        @RestQuery
        @DefaultValue("20")
        protected Integer size;

        public abstract String describe();
    }

    public static class UserFilter extends PageFilter {

        @RestQuery
        private String username;

        @RestQuery
        private String fullName;

        @Override
        public String describe() {
            return "number=" + number + ",size=" + size + ",username=" + username + ",fullName=" + fullName;
        }
    }

    public interface PageResource<F extends PageFilter> {

        @GET
        @Path("/list")
        @Produces(MediaType.TEXT_PLAIN)
        default String list(@BeanParam F filter) {
            return filter.describe();
        }
    }

    @Path("/api/user")
    public static class UserResource implements PageResource<UserFilter> {
    }
}
