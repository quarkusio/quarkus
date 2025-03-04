package io.quarkus.rest.client.reactive.subresource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.PreloadClassBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class SubResourceNoDuplicateClassGenerationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(StoreResourceClientV2.class)
                    .addClass(StoreResourceClient.class)
                    .addClass(OrderResourceClient.class)
                    .addClass(PositionResourceClient.class))
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            Map<Class<?>, List<String>> genClientsForInterface = new HashMap<>();
                            List<GeneratedClassBuildItem> generatedClassBuildItems = context
                                    .consumeMulti(GeneratedClassBuildItem.class);
                            for (GeneratedClassBuildItem generatedClassBuildItem : generatedClassBuildItems) {
                                Class<?> key = null;
                                if (generatedClassBuildItem.binaryName().contains(StoreResourceClientV2.class.getName())) {
                                    key = StoreResourceClientV2.class;
                                } else if (generatedClassBuildItem.binaryName().contains(StoreResourceClient.class.getName())) {
                                    key = StoreResourceClient.class;
                                } else if (generatedClassBuildItem.binaryName().contains(OrderResourceClient.class.getName())) {
                                    key = OrderResourceClient.class;
                                } else if (generatedClassBuildItem.binaryName()
                                        .contains(PositionResourceClient.class.getName())) {
                                    key = PositionResourceClient.class;
                                }
                                if (key != null) {
                                    // List<classname> instead of a simple count to simplify test debugging
                                    genClientsForInterface.computeIfAbsent(key, ignored -> new ArrayList<>())
                                            .add(generatedClassBuildItem.binaryName());
                                }

                            }

                            // Each store resource includes the order resource with different query params
                            // meaning different sub resources impls need to be generated

                            // For PositionResourceClient, only 4 as well. Because
                            // StoreResourceClient -> OrderResourceClient -> PositionResourceClient
                            // and OrderResourceClient -> PositionResourceClient
                            // share the same set of not path parameters

                            // invoker + client
                            assertThat(genClientsForInterface, hasEntry(equalTo(StoreResourceClientV2.class), hasSize(2)));
                            // invoker + client
                            assertThat(genClientsForInterface, hasEntry(equalTo(StoreResourceClient.class), hasSize(2)));
                            // invoker + client + 2 (1 for each store resource)
                            assertThat(genClientsForInterface, hasEntry(equalTo(OrderResourceClient.class), hasSize(4)));
                            // invoker + client + 2 (1 for each store resource)
                            assertThat(genClientsForInterface, hasEntry(equalTo(PositionResourceClient.class), hasSize(4)));
                        }
                        // just claim to produce PreloadClassBuildItem to get this build step to run
                    }).consumes(GeneratedClassBuildItem.class).produces(PreloadClassBuildItem.class).build();
                }
            });

    @Test
    void dummy() {
        // test logic is in build step
    }

    @Path("store")
    public interface StoreResourceClientV2 {

        @Path("orders/{orderId}")
        OrderResourceClient orderResource(@RestPath String orderId);
    }

    @Path("store")
    public interface StoreResourceClient {

        // In this fictive scenario, the Store SAAS decided to remove the customerId from the query params, and instead retrieve it using a jwt
        // Now image these Client interfaces are part of a store-client module provided by the saas. They could still keep this
        // (now deprecated) client around, as to not break client module consumers.
        // On the quarkus side, we need to make sure that distinct sub resources for the orderresourceclient (and its subresources) are generated
        // to make it possible to pass the customerId QueryParam around
        @Path("orders/{orderId}")
        OrderResourceClient orderResource(@RestQuery String customerId, @RestPath String orderId);
    }

    public interface OrderResourceClient {
        @Path("positions")
        PositionResourceClient positionResource();
    }

    public interface PositionResourceClient {
        @GET
        List<String> listAll();
    }
}
