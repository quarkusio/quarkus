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
public final class KubernetesJobBuildItem extends MultiBuildItem {

    private final String name;
    private final String image;
    private final List<String> command;
    private final List<String> arguments;
    private final Map<String, String> envVars;
    private final boolean sharedEnvironment;
    private final boolean sharedFilesystem;

    public static KubernetesJobBuildItem create(String image) {
        return new KubernetesJobBuildItem("init", image, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), false, false);
    }

    public KubernetesJobBuildItem(String name, String image, List<String> command, List<String> arguments,
            Map<String, String> envVars, boolean sharedEnvironment, boolean sharedFilesystem) {
        this.name = name;
        this.image = image;
        this.command = command;
        this.arguments = arguments;
        this.envVars = envVars;
        this.sharedEnvironment = sharedEnvironment;
        this.sharedFilesystem = sharedFilesystem;
    }

    public String getName() {
        return name;
    }

    public KubernetesJobBuildItem withName(String name) {
        return new KubernetesJobBuildItem(name, image, command, arguments, envVars, sharedEnvironment, sharedFilesystem);
    }

    public String getImage() {
        return image;
    }

    public KubernetesJobBuildItem withImage(String image) {
        return new KubernetesJobBuildItem(name, image, command, arguments, envVars, sharedEnvironment, sharedFilesystem);
    }

    public List<String> getCommand() {
        return command;
    }

    public KubernetesJobBuildItem withCommand(List<String> command) {
        return new KubernetesJobBuildItem(name, image, command, arguments, envVars, sharedEnvironment, sharedFilesystem);
    }

    public List<String> getArguments() {
        return arguments;
    }

    public KubernetesJobBuildItem withArguments(List<String> arguments) {
        return new KubernetesJobBuildItem(name, image, command, arguments, envVars, sharedEnvironment, sharedFilesystem);
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public KubernetesJobBuildItem withEnvVars(Map<String, String> envVars) {
        return new KubernetesJobBuildItem(name, image, command, arguments, envVars, sharedEnvironment, sharedFilesystem);
    }

    public boolean isSharedEnvironment() {
        return sharedEnvironment;
    }

    public KubernetesJobBuildItem withSharedEnvironment(boolean sharedEnvironment) {
        return new KubernetesJobBuildItem(name, image, command, arguments, envVars, sharedEnvironment, sharedFilesystem);
    }

    public boolean isSharedFilesystem() {
        return sharedFilesystem;
    }

    public KubernetesJobBuildItem withSharedFilesystem(boolean sharedFilesystem) {
        return new KubernetesJobBuildItem(name, image, command, arguments, envVars, sharedEnvironment, sharedFilesystem);
    }
}
