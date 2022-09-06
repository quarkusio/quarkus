package io.quarkus.resteasy.reactive.server.test.beanparam;

import static org.hamcrest.CoreMatchers.equalTo;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Related to the issue: https://github.com/quarkusio/quarkus/issues/27501
 */
public class NestedBeanParamInSubResourcesTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResource.class, SearchResource.class, City.class, Country.class));

    @Test
    void shouldParseNestedBeanParams() {
        RestAssured.get("/test/city/search?name=Malaga&country=Spain")
                .then()
                .statusCode(200)
                .body(equalTo("Got: Malaga, Spain"));
    }

    @Path("/test")
    public static class TestResource {

        @Path("/city")
        public SearchResource searchCities() {
            return new SearchResource();
        }

    }

    public static class SearchResource {

        @GET
        @Path("/search")
        @Produces(MediaType.TEXT_PLAIN)
        public String search(@BeanParam City city) {
            return "Got: " + city.name + ", " + city.country.country;
        }
    }

    public static class City {

        @QueryParam("name")
        String name;

        @BeanParam
        Country country;
    }

    public static class Country {
        @QueryParam("country")
        String country;
    }
}
