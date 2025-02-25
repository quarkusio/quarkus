package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildStepBuilder;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;

/**
 * A marker build item that indicates, if any instances are provided during the build, the containers started by DevServices
 * may use a shared network.
 * This is mainly useful in integration tests where the application container needs to be able
 * to communicate with the service containers.
 */
public final class DevServicesSharedNetworkBuildItem extends MultiBuildItem {

    private final String source;

    /** Create a build item without identifying the creator source. */
    public DevServicesSharedNetworkBuildItem() {
        this.source = UNKNOWN_SOURCE;
    }

    /**
     * Create a build item identifying the creator source.
     *
     * @param source The identifier of the creator
     */
    public DevServicesSharedNetworkBuildItem(String source) {
        this.source = source;
    }

    /** The creator source of this build item. May be useful to decide whether a DevService should join a shared network. */
    public String getSource() {
        return source;
    }

    /* Property used by factory to retrieve the source of instanciation. */
    public static final String SOURCE_PROPERTY = "source";

    /* Value of source field when instanciation origin is unknown. */
    public static final String UNKNOWN_SOURCE = "unknown";

    /**
     * Generates a {@code List<Consumer<BuildChainBuilder>> build chain builder} which creates a build step
     * producing the {@link DevServicesSharedNetworkBuildItem} build item.
     */
    public static final class Factory implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

        @Override
        public List<Consumer<BuildChainBuilder>> apply(final Map<String, Object> props) {
            return Collections.singletonList((builder) -> {
                BuildStepBuilder stepBuilder = builder.addBuildStep((ctx) -> {
                    DevServicesSharedNetworkBuildItem buildItem;
                    if (props != null && props.containsKey(SOURCE_PROPERTY)) {
                        buildItem = new DevServicesSharedNetworkBuildItem(props.get(SOURCE_PROPERTY).toString());
                    } else {
                        buildItem = new DevServicesSharedNetworkBuildItem();
                    }
                    ctx.produce(buildItem);
                });
                stepBuilder.produces(DevServicesSharedNetworkBuildItem.class).build();
            });
        }
    }

    /**
     * Helper method for DevServices processors that tells if joining the shared network is required.
     * Joining this network may be required if explicitily asked by user properties or if running a containerized
     * application during integration tests.
     */
    public static boolean isSharedNetworkRequired(
            DevServicesConfig devServicesConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {
        return devServicesConfig.launchOnSharedNetwork() ||
                (!devServicesSharedNetworkBuildItem.isEmpty()
                        && devServicesSharedNetworkBuildItem.get(0).getSource().equals("io.quarkus.test.junit"));
    }

    /**
     * @deprecated Please, use {@link DevServicesSharedNetworkBuildItem#isSharedNetworkRequired(DevServicesConfig, List)}
     *             instead.
     */
    @Deprecated(forRemoval = true, since = "3.18")
    public static boolean isSharedNetworkRequired(
            GlobalDevServicesConfig globalDevServicesConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {
        return globalDevServicesConfig.launchOnSharedNetwork ||
                (!devServicesSharedNetworkBuildItem.isEmpty()
                        && devServicesSharedNetworkBuildItem.get(0).getSource().equals("io.quarkus.test.junit"));
    }
}
