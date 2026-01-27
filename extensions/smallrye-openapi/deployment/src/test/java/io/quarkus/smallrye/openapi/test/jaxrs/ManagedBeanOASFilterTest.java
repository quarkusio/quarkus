package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.OpenApiFilter.RunStage;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpServerRequest;

class ManagedBeanOASFilterTest {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyFilter1.class, MyFilter2.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.basic=true
                            quarkus.http.auth.permission.basic.paths=/*
                            quarkus.http.auth.permission.basic.policy=authenticated
                            quarkus.security.users.embedded.enabled=true
                            quarkus.security.users.embedded.plain-text=true
                            quarkus.security.users.embedded.users.alice=alice
                            quarkus.security.users.embedded.users.bob=bob
                            quarkus.smallrye-openapi.always-run-filter=true
                            """),
                            "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-security", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-vertx-http", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file", Version.getVersion())));

    @RequestScoped
    @OpenApiFilter(value = RunStage.RUN, priority = 99)
    public static class MyFilter1 implements OASFilter {
        @Inject
        HttpServerRequest req;

        @Inject
        SecurityIdentity principal;

        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addTag(OASFactory.createTag()
                    .name("customized-for-" + principal.getPrincipal())
                    .description("custom: " + req.getParam("custom")));
        }
    }

    @ApplicationScoped
    @OpenApiFilter(value = RunStage.RUN, priority = 98)
    public static class MyFilter2 implements OASFilter {
        @Inject
        HttpServerRequest req;

        @Inject
        SecurityIdentity principal;

        AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addTag(OASFactory.createTag()
                    .name("tag-sequence")
                    .description(Integer.toString(counter.incrementAndGet())));
        }
    }

    @Test
    void testOpenApiPathAccessResource() {
        RestAssured
                .given()
                .queryParam("format", "JSON")
                .queryParam("custom", "special-value-for-alice")
                .auth().basic("alice", "alice")
                .when()
                .get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("tags[0].name", is("customized-for-alice"))
                .body("tags[0].description", is("custom: special-value-for-alice"))
                .body("tags[1].name", is("tag-sequence"))
                .body("tags[1].description", is("1"));
        RestAssured
                .given()
                .queryParam("format", "JSON")
                .queryParam("custom", "special-value-for-bob")
                .auth().basic("bob", "bob")
                .when()
                .get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("tags[0].name", is("customized-for-bob"))
                .body("tags[0].description", is("custom: special-value-for-bob"))
                .body("tags[1].name", is("tag-sequence"))
                .body("tags[1].description", is("2"));
    }
}
