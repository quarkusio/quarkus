package io.quarkus.rest.client.reactive;

import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.hamcrest.MatcherAssert;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class InterfacePathInheritanceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Z.class)
                    .addClass(Y.class)
                    .addClass(InheritanceTestClient.class))
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            ResourceScanningResultBuildItem consumed = context.consume(ResourceScanningResultBuildItem.class);
                            context.produce(new FeatureBuildItem("just-here-to-invoke-buildstep"));

                            Map<DotName, String> clientInterfaces = consumed.getResult().getClientInterfaces();
                            MatcherAssert.assertThat(clientInterfaces.size(), is(3));
                            clientInterfaces.forEach((k, v) -> {
                                MatcherAssert.assertThat("Path of %s needs to match".formatted(k), v, is("hello"));
                            });
                        }
                    }).consumes(ResourceScanningResultBuildItem.class).produces(FeatureBuildItem.class).build();
                }
            });

    @Test
    void basicTest() {
        // see addBuildChainCustomizer of RegisterExtension for the test logic
    }

    @Path("hello")
    public interface Z {
        @GET
        @Path("something")
        String get();
    }

    public interface Y extends Z {

    }

    @RegisterRestClient
    public interface InheritanceTestClient extends Y {

    }
}
