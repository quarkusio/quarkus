package io.quarkus.kubernetes.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A built item for generating init containers.
 * The generated container will have the specified fields
 * and may optionally inherit env vars and volumes from the app container.
 *
 * Env vars specified through this build item, will take precedence over inherited ones.
 */
public final class KubernetesInitContainerBuildItem extends MultiBuildItem {

    private final String name;
    private final String image;
    private final List<String> command;
    private final List<String> arguments;
    private final Map<String, String> envVars;
    private final boolean inheritEnvVars;
    private final boolean inheritMounts;

    public static KubernetesInitContainerBuildItem create(String image) {
        return new KubernetesInitContainerBuildItem("init", image, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), false, false);
    }

    public KubernetesInitContainerBuildItem(String name, String image, List<String> command, List<String> arguments,
            Map<String, String> envVars, boolean inheritEnvVars, boolean inheritMounts) {
        this.name = name;
        this.image = image;
        this.command = command;
        this.arguments = arguments;
        this.envVars = envVars;
        this.inheritEnvVars = inheritEnvVars;
        this.inheritMounts = inheritMounts;
    }

    public String getName() {
        return name;
    }

    public KubernetesInitContainerBuildItem withName(String name) {
        return new KubernetesInitContainerBuildItem(name, image, command, arguments, envVars, inheritEnvVars, inheritMounts);
    }

    public String getImage() {
        return image;
    }

    public KubernetesInitContainerBuildItem withImage(String image) {
        return new KubernetesInitContainerBuildItem(name, image, command, arguments, envVars, inheritEnvVars, inheritMounts);
    }

    public List<String> getCommand() {
        return command;
    }

    public KubernetesInitContainerBuildItem withCommand(List<String> command) {
        return new KubernetesInitContainerBuildItem(name, image, command, arguments, envVars, inheritEnvVars, inheritMounts);
    }

    public List<String> getArguments() {
        return arguments;
    }

    public KubernetesInitContainerBuildItem withArguments(List<String> arguments) {
        return new KubernetesInitContainerBuildItem(name, image, command, arguments, envVars, inheritEnvVars, inheritMounts);
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public KubernetesInitContainerBuildItem withEnvVars(Map<String, String> envVars) {
        return new KubernetesInitContainerBuildItem(name, image, command, arguments, envVars, inheritEnvVars, inheritMounts);
    }

    public boolean isInheritEnvVars() {
        return inheritEnvVars;
    }

    public KubernetesInitContainerBuildItem withInheritEnvVars(boolean inheritEnvVars) {
        return new KubernetesInitContainerBuildItem(name, image, command, arguments, envVars, inheritEnvVars, inheritMounts);
    }

    public boolean isInheritMounts() {
        return inheritMounts;
    }

    public KubernetesInitContainerBuildItem withInheritMounts(boolean inheritMounts) {
        return new KubernetesInitContainerBuildItem(name, image, command, arguments, envVars, inheritEnvVars, inheritMounts);
    }
}
