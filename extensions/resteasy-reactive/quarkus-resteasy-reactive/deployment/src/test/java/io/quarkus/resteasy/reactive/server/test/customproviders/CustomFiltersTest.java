package io.quarkus.resteasy.reactive.server.test.customproviders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Headers;

public class CustomFiltersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(CustomContainerRequestFilter.class, CustomFiltersResource.class,
                                    CustomContainerResponseFilter.class, AssertContainerFilter.class, SomeBean.class,
                                    Metal.class, MetalFilter.class);
                }
            });

    @Test
    public void testFilters() {
        Headers headers = RestAssured.given().header("some-input", "bar").get("/custom/req")
                .then().statusCode(200).body(Matchers.containsString("/custom/req-bar-null")).extract().headers();
        assertThat(headers.getValues("java-method")).containsOnly("filters");
        Assertions.assertEquals(3, AssertContainerFilter.COUNT.get());

        headers = RestAssured.given().header("some-input", "bar").get("/custom/metal")
                .then().statusCode(200).body(Matchers.containsString("/custom/metal-bar-metal")).extract().headers();
        assertThat(headers.getValues("java-method")).containsOnly("metal");
    }
}
