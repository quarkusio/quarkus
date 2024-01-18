package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.info.runtime.spi.InfoContributor;
import io.quarkus.test.QuarkusUnitTest;

public class ExternalInfoContributorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .addBuildChainCustomizer(
                    buildChainBuilder -> buildChainBuilder.addBuildStep(
                            context -> new AdditionalBeanBuildItem(TestInfoContributor.class))
                            .produces(AdditionalBeanBuildItem.class)
                            .build())
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestInfoContributor.class));

    @Test
    public void test() {
        when().get("/q/info")
                .then()
                .statusCode(200)
                .body("os", is(notNullValue()))
                .body("os.name", is(notNullValue()))
                .body("java", is(notNullValue()))
                .body("java.version", is(notNullValue()))
                .body("build", is(notNullValue()))
                .body("build.time", is(notNullValue()))
                .body("git", is(notNullValue()))
                .body("git.branch", is(notNullValue()))
                .body("test", is(notNullValue()))
                .body("test.foo", is("bar"));

    }

    @ApplicationScoped
    public static class TestInfoContributor implements InfoContributor {

        @Override
        public String name() {
            return "test";
        }

        @Override
        public Map<String, Object> data() {
            return Map.of("foo", "bar");
        }
    }
}
