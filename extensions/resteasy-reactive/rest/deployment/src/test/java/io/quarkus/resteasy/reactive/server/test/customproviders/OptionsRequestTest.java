package io.quarkus.resteasy.reactive.server.test.customproviders;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerResponseContext;

import org.assertj.core.api.Assertions;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class OptionsRequestTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, Filters.class);
                }
            });

    @Test
    public void testJsonHeaderAdded() {
        String allowValue = when()
                .options("/test")
                .then()
                .statusCode(200)
                .header("Foo", equalTo("Bar"))
                .body(is(emptyOrNullString()))
                .extract().header("Allow");

        Assertions.assertThat(allowValue).contains("GET", "HEAD", "OPTIONS");
    }

    @Path("test")
    public static class Resource {

        @GET
        public String hello() {
            return "hello";
        }
    }

    public static class Filters {
        @ServerResponseFilter
        public void preMatchingFilter(ContainerResponseContext context) {
            context.getHeaders().putSingle("Foo", "Bar");
        }
    }
}
