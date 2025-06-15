package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;
import io.restassured.http.Headers;

public class CustomFiltersTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(CustomContainerRequestFilter.class,
                    CustomFiltersResource.class, CustomContainerResponseFilter.class, AssertContainerFilter.class,
                    SomeBean.class, Metal.class, MetalFilter.class);
        }
    });

    @Test
    public void testFilters() {
        Headers headers = RestAssured.given().header("some-input", "bar").get("/custom/req").then().statusCode(200)
                .body(Matchers.containsString("/custom/req-bar-null")).extract().headers();
        assertThat(headers.getValues("java-method")).containsOnly("filters");
        assertThat(headers.getValues("h1")).containsOnly("true");
        assertThat(headers.getValues("h2")).containsOnly("true");
        assertThat(headers.getValues("h3")).containsOnly("true");

        headers = RestAssured.given().header("some-input", "bar").get("/custom/metal").then().statusCode(200)
                .body(Matchers.containsString("/custom/metal-bar-metal")).extract().headers();
        assertThat(headers.getValues("java-method")).containsOnly("metal");
        assertThat(headers.getValues("h1")).containsOnly("true");
        assertThat(headers.getValues("h2")).containsOnly("true");
        assertThat(headers.getValues("h3")).containsOnly("true");
    }
}
