package io.quarkus.rest.test.customproviders;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomerProvidersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(CustomContainerRequestFilter.class, CustomerProvidersResource.class,
                                    AssertContainerRequestFilter.class, SomeBean.class);
                }
            });

    @Test
    public void testFilters() {
        RestAssured.given().header("some-input", "bar").get("/custom/req")
                .then().statusCode(200).body(Matchers.containsString("/custom/req-bar"));
        Assertions.assertEquals(2, AssertContainerRequestFilter.COUNT.get());
    }
}
