package io.quarkus.kubernetes.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A Built item for generating init containers.
 * The generated container will have the specified fields
 * and may optionally inherit env vars and volumes from the app container.
 * <p>
 * Env vars specified through this build item, will take precedence over inherited ones.
 */
public final class KubernetesJobBuildItem extends MultiBuildItem implements Targetable {

    private final String name;
    private final String target;
    private final String image;
    private final List<String> command;
    private final List<String> arguments;
    private final Map<String, String> envVars;
    private final boolean sharedEnvironment;
    private final boolean sharedFilesystem;

    public static KubernetesJobBuildItem create(String image) {
        return new KubernetesJobBuildItem("init", null, image, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), false, false);
    }

    public KubernetesJobBuildItem(String name, String target, String image, List<String> command, List<String> arguments,
            Map<String, String> envVars, boolean sharedEnvironment, boolean sharedFilesystem) {
        this.name = name;
        this.target = target;
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
        return new KubernetesJobBuildItem(name, target, image, command, arguments, envVars, sharedEnvironment,
                sharedFilesystem);
    }

    public String getTarget() {
        return target;
    }

    public KubernetesJobBuildItem withTarget(String target) {
        return new KubernetesJobBuildItem(name, target, image, command, arguments, envVars, sharedEnvironment,
                sharedFilesystem);
    }

    public String getImage() {
        return image;
    }

    @SuppressWarnings("unused")
    public KubernetesJobBuildItem withImage(String image) {
        return new KubernetesJobBuildItem(name, target, image, command, arguments, envVars, sharedEnvironment,
                sharedFilesystem);
    }

    public List<String> getCommand() {
        return command;
    }

    public KubernetesJobBuildItem withCommand(List<String> command) {
        return new KubernetesJobBuildItem(name, target, image, command, arguments, envVars, sharedEnvironment,
                sharedFilesystem);
    }

    public List<String> getArguments() {
        return arguments;
    }

    public KubernetesJobBuildItem withArguments(List<String> arguments) {
        return new KubernetesJobBuildItem(name, target, image, command, arguments, envVars, sharedEnvironment,
                sharedFilesystem);
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public KubernetesJobBuildItem withEnvVars(Map<String, String> envVars) {
        return new KubernetesJobBuildItem(name, target, image, command, arguments, envVars, sharedEnvironment,
                sharedFilesystem);
    }

    /**
     * Flag for tasks that require access to the environment variables of the application.
     * Often tasks need to access resources, configured via environment variables. This
     * flag expresses that the task should be executed using the same envrironment variables as the application.
     *
     * @return true when the task is meant to share environment variables with the application.
     */
    public boolean isSharedEnvironment() {
        return sharedEnvironment;
    }

    public KubernetesJobBuildItem withSharedEnvironment(boolean sharedEnvironment) {
        return new KubernetesJobBuildItem(name, target, image, command, arguments, envVars, sharedEnvironment,
                sharedFilesystem);
    }

    /**
     * Flag for tasks that need to share filesystem with the application.
     * Often tasks need to access resources, configured via filesystem (e.g. local config files, kubernetes service binding
     * etc).
     * In other cases, tasks may need to produce files needed by the application.
     * This flag expresses that the task should share filesystem with the application.
     *
     * @return true when the task is meant to share filesystem.
     */
    public boolean isSharedFilesystem() {
        return sharedFilesystem;
    }

    public KubernetesJobBuildItem withSharedFilesystem(boolean sharedFilesystem) {
        return new KubernetesJobBuildItem(name, target, image, command, arguments, envVars, sharedEnvironment,
                sharedFilesystem);
    }
}
