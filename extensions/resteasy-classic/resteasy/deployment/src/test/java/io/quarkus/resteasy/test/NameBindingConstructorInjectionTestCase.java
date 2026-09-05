package io.quarkus.resteasy.test;

import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * A resource using constructor injection has no no-arg constructor, so Quarkus generates a synthetic one for RESTEasy
 * Classic. A class-level {@link jakarta.ws.rs.NameBinding} annotation used to prevent that generation, making startup
 * fail with {@code RESTEASY003190: Could not find constructor}. This verifies both the class-level and method-level
 * binding placements work with constructor injection, and that the name-bound filter is actually applied only where the
 * binding is present.
 */
public class NameBindingConstructorInjectionTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ClassLevelNameBindingResource.class, MethodLevelNameBindingResource.class,
                            NoNameBindingResource.class, Hello.class, HelloFilter.class, Service.class));

    @Test
    public void testClassLevelNameBindingWithConstructorInjection() {
        RestAssured.when().get("/ctor-namebinding").then()
                .statusCode(200)
                .body(Matchers.is("service"))
                .header("X-Hello", "true");
    }

    @Test
    public void testMethodLevelNameBindingWithConstructorInjection() {
        RestAssured.when().get("/method-namebinding").then()
                .statusCode(200)
                .body(Matchers.is("service"))
                .header("X-Hello", "true");
    }

    @Test
    public void testFilterNotAppliedWithoutNameBinding() {
        // negative control: the name-bound filter must not run on a resource without the binding, so a passing
        // class/method-level test above genuinely proves the binding is honored rather than the filter being global
        RestAssured.when().get("/no-namebinding").then()
                .statusCode(200)
                .body(Matchers.is("service"))
                .header("X-Hello", nullValue());
    }
}
