package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.resteasy.reactive.server.spi.AllowNotRestParametersBuildItem;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.MixedParameterResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test resources with not all method parameters related to RESTEasy.
 */
@DisplayName("Allow Not RESTEasy Method Parameters")
public class AllowNotResteasyParametersTest {

    @RegisterExtension
    static QuarkusUnitTest quarkusUnitTest = new QuarkusUnitTest()
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new AllowNotRestParametersBuildItem());
                        }
                    }).produces(AllowNotRestParametersBuildItem.class).build();
                }
            })
            .withApplicationRoot((jar) -> jar
                    .addClass(MixedParameterResource.class));

    @Test
    @DisplayName("Test Resource Method with one param not related to RESTEasy")
    public void shouldOkEvenNotResteasyParameterPresence() {
        given()
                .body("value")
                .post("/" + MixedParameterResource.class.getSimpleName() + "/mixed?foo=bar")
                .then().statusCode(200).body(is("bar.value"));
    }

    @Test
    @DisplayName("Test Resource Method with only one param not related to RESTEasy")
    public void shouldOkEvenNotResteasySingleParameterPresence() {
        given()
                .body("value")
                .get("/" + MixedParameterResource.class.getSimpleName() + "/single")
                .then().statusCode(200);
    }
}
