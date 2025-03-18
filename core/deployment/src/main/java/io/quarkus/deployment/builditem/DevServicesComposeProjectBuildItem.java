package io.quarkus.deployment.builditem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.dev.devservices.ContainerInfo;
import io.quarkus.deployment.dev.devservices.ImageName;
import io.quarkus.deployment.dev.devservices.RunningContainer;

/**
 * BuildItem for running services provided by compose
 */
public final class DevServicesComposeProjectBuildItem extends SimpleBuildItem {

    public static final String COMPOSE_IGNORE = "io.quarkus.devservices.compose.ignore";

    private final String project;

    private final String defaultNetworkId;

    private final Map<String, List<RunningContainer>> composeServices;

    private final Map<String, String> config;

    public DevServicesComposeProjectBuildItem() {
        this(null, null, Collections.emptyMap(), Collections.emptyMap());
    }

    public DevServicesComposeProjectBuildItem(String project,
            String defaultNetworkId,
            Map<String, List<RunningContainer>> composeServices,
            Map<String, String> config) {
        this.project = project;
        this.defaultNetworkId = defaultNetworkId;
        this.composeServices = composeServices;
        this.config = config;
    }

    public String getProject() {
        return project;
    }

    public String getDefaultNetworkId() {
        return defaultNetworkId;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public Map<String, List<RunningContainer>> getComposeServices() {
        return composeServices;
    }

    /**
     * Locate a running container by image partial and port
     * The container image partial can be a substring of the full image name
     *
     * @param imagePartials image partials
     * @param port exposed port
     * @return the running container or null if not found
     */
    public Optional<RunningContainer> locate(List<String> imagePartials, int port) {
        return locateContainers(imagePartials)
                .filter(r -> Optional.ofNullable(r.containerInfo().exposedPorts())
                        .map(ports -> Arrays.stream(ports)
                                .anyMatch(p -> Objects.equals(p.privatePort(), port)))
                        .isPresent())
                .findFirst();
    }

    /**
     * Locate a running container by image partial
     * The container image partial can be a substring of the full image name
     * Ignored services are not returned
     *
     * @param imagePartials image partials
     * @return the list of running containers
     */
    public List<RunningContainer> locate(List<String> imagePartials) {
        return locateContainers(imagePartials).toList();
    }

    /**
     * Locate the first running container by image partial
     * The container image partial can be a substring of the full image name
     * Ignored services are not returned
     *
     * @param imagePartials image partials
     * @return the first running container
     */
    public Optional<RunningContainer> locateFirst(List<String> imagePartials) {
        return locateContainers(imagePartials).findFirst();
    }

    private Stream<RunningContainer> locateContainers(List<String> imagePartials) {
        return imagePartials.stream()
                .flatMap(imagePartial -> composeServices.values().stream().flatMap(List::stream)
                        // Ignore service if contains ignore label
                        .filter(c -> !isContainerIgnored(c.containerInfo()))
                        .filter(runningContainer -> {
                            String imageName = runningContainer.containerInfo().imageName();
                            return imageName.contains(imagePartial)
                                    || ImageName.parse(imageName).withLibraryPrefix().toString().contains(imagePartial);
                        }));
    }

    /**
     * Ignored services are not returned by locate
     *
     * @param containerInfo container info
     * @return true if the container should be ignored
     */
    public static boolean isContainerIgnored(ContainerInfo containerInfo) {
        return Boolean.parseBoolean(containerInfo.labels().get(COMPOSE_IGNORE));
    }
}
