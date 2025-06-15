package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.info.deployment.spi.InfoBuildTimeContributorBuildItem;
import io.quarkus.info.deployment.spi.InfoBuildTimeValuesBuildItem;
import io.quarkus.info.runtime.spi.InfoContributor;
import io.quarkus.test.QuarkusUnitTest;

public class CustomDataInfoTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestInfoContributor.class))
            .addBuildChainCustomizer(buildCustomizer());

    protected static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            // This represents the extension.
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(context -> {
                    context.produce(new InfoBuildTimeContributorBuildItem(new TestInfoContributor()));
                    context.produce(new InfoBuildTimeValuesBuildItem("test2", Map.of("key", "value")));
                }).produces(InfoBuildTimeContributorBuildItem.class).produces(InfoBuildTimeValuesBuildItem.class)
                        .build();
            }
        };
    }

    @Test
    public void test() {
        when().get("/q/info").then().statusCode(200).body("os", is(notNullValue())).body("os.name", is(notNullValue()))
                .body("java", is(notNullValue())).body("java.version", is(notNullValue()))
                .body("build", is(notNullValue())).body("build.time", is(notNullValue()))
                .body("git", is(notNullValue())).body("git.branch", is(notNullValue())).body("test", is(notNullValue()))
                .body("test.foo", is("bar")).body("test2", is(notNullValue())).body("test2.key", is("value"));

    }

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
