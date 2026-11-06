package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.rest.client.reactive.spi.RegisteredRestClientBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies the contract of {@link RegisteredRestClientBuildItem} other extensions rely on.
 */
@SuppressWarnings("removal")
public class RegisteredRestClientBuildItemTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(BaseClient.class)
                    .addClass(InheritingClient.class))
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            List<RegisteredRestClientBuildItem> restClients = context
                                    .consumeMulti(RegisteredRestClientBuildItem.class);
                            context.produce(new FeatureBuildItem("just-here-to-invoke-buildstep"));

                            assertThat(restClients).hasSize(1);
                            RegisteredRestClientBuildItem restClient = restClients.get(0);
                            assertThat(restClient.getClassInfo().name()).isEqualTo(dotName(InheritingClient.class));
                            assertThat(restClient.getConfigKey()).contains("inheriting");
                            assertThat(restClient.getDefaultBaseUri()).contains("http://localhost:8080");
                            assertThat(restClient.getCdiWrapperName())
                                    .isEqualTo(DotName.createSimple(InheritingClient.class.getName() + "$$CDIWrapper"));

                            // the JAX-RS methods of the interface and its super interfaces come first, then the default methods
                            List<MethodInfo> methods = restClient.getCdiWrapperMethods();
                            assertThat(methods).extracting(MethodInfo::name)
                                    .containsExactly("own", "inherited", "withDefault");
                            // each method is reported as declared, so the declaring class of an inherited method
                            // is the super interface
                            assertThat(methods).extracting(m -> m.declaringClass().name())
                                    .containsExactly(dotName(InheritingClient.class), dotName(BaseClient.class),
                                            dotName(InheritingClient.class));

                            // the deprecated build item is still produced for extensions that have not migrated yet
                            List<io.quarkus.rest.client.reactive.deployment.RegisteredRestClientBuildItem> deprecated = context
                                    .consumeMulti(
                                            io.quarkus.rest.client.reactive.deployment.RegisteredRestClientBuildItem.class);
                            assertThat(deprecated).hasSize(1);
                            assertThat(deprecated.get(0).getClassInfo().name()).isEqualTo(dotName(InheritingClient.class));
                            assertThat(deprecated.get(0).getConfigKey()).contains("inheriting");
                            assertThat(deprecated.get(0).getDefaultBaseUri()).contains("http://localhost:8080");
                        }
                    }).consumes(RegisteredRestClientBuildItem.class)
                            .consumes(io.quarkus.rest.client.reactive.deployment.RegisteredRestClientBuildItem.class)
                            .produces(FeatureBuildItem.class).build();
                }
            });

    private static DotName dotName(Class<?> clazz) {
        return DotName.createSimple(clazz.getName());
    }

    @Test
    void buildItemContract() {
        // see addBuildChainCustomizer of RegisterExtension for the test logic
    }

    @Path("/base")
    public interface BaseClient {
        @GET
        String inherited();
    }

    @RegisterRestClient(configKey = "inheriting", baseUri = "http://localhost:8080")
    @Path("/inheriting")
    public interface InheritingClient extends BaseClient {
        @GET
        String own();

        default String withDefault() {
            return own() + inherited();
        }
    }
}
