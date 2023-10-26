package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildStepBuilder;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * A marker build item that indicates, if any instances are provided during the build, the containers started by DevServices
 * will use a shared network.
 * This is mainly useful in integration tests where the application container needs to be able
 * to communicate with the service containers.
 */
public final class DevServicesSharedNetworkBuildItem extends MultiBuildItem {

    /**
     * Generates a {@code List<Consumer<BuildChainBuilder>> build chain builder} which creates a build step
     * producing the {@link DevServicesSharedNetworkBuildItem} build item.
     */
    public static final class Factory implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

        @Override
        public List<Consumer<BuildChainBuilder>> apply(final Map<String, Object> props) {
            return Collections.singletonList((builder) -> {
                BuildStepBuilder stepBuilder = builder.addBuildStep((ctx) -> {
                    ctx.produce(new DevServicesSharedNetworkBuildItem());
                });
                stepBuilder.produces(DevServicesSharedNetworkBuildItem.class).build();
            });
        }
    }
}
