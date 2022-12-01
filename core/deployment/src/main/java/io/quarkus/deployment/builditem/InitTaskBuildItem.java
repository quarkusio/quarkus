package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents an initalization task for the application.
 * Often extension perform some short of initialization as part of the application startup.
 * There are cases where we we want to externalize the initialization (e.g. in a pipeline).
 *
 * Often the task is run using the same artifact as the application but using a different command or
 * arguments. In the later case it might be deseriable to pass addtinal environment variable to both the
 * init tasks (to enable init) and the application (to disable the init).
 */
public final class InitTaskBuildItem extends MultiBuildItem {

    private final String name;
    private final Optional<String> image;
    private final List<String> command;
    private final List<String> arguments;
    private final Map<String, String> taskEnvVars;
    private final Map<String, String> appEnvVars;
    private final boolean sharedEnvironment;
    private final boolean sharedFilesystem;

    public static InitTaskBuildItem create() {
        return new InitTaskBuildItem("init", Optional.empty(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), Collections.emptyMap(), false, false);
    }

    public InitTaskBuildItem(String name, Optional<String> image, List<String> command, List<String> arguments,
            Map<String, String> taskEnvVars, Map<String, String> appEnvVars, boolean sharedEnvironment,
            boolean sharedFilesystem) {
        this.name = name;
        this.image = image;
        this.command = command;
        this.arguments = arguments;
        this.taskEnvVars = taskEnvVars;
        this.appEnvVars = appEnvVars;
        this.sharedEnvironment = sharedEnvironment;
        this.sharedFilesystem = sharedFilesystem;
    }

    public String getName() {
        return name;
    }

    public InitTaskBuildItem withName(String name) {
        return new InitTaskBuildItem(name, image, command, arguments, taskEnvVars, appEnvVars, sharedEnvironment,
                sharedFilesystem);
    }

    public Optional<String> getImage() {
        return image;
    }

    public InitTaskBuildItem withImage(String image) {
        return new InitTaskBuildItem(name, Optional.of(image), command, arguments, taskEnvVars, appEnvVars, sharedEnvironment,
                sharedFilesystem);
    }

    public List<String> getCommand() {
        return command;
    }

    public InitTaskBuildItem withCommand(List<String> command) {
        return new InitTaskBuildItem(name, image, command, arguments, taskEnvVars, appEnvVars, sharedEnvironment,
                sharedFilesystem);
    }

    public List<String> getArguments() {
        return arguments;
    }

    public InitTaskBuildItem withArguments(List<String> arguments) {
        return new InitTaskBuildItem(name, image, command, arguments, taskEnvVars, appEnvVars, sharedEnvironment,
                sharedFilesystem);
    }

    public Map<String, String> getTaskEnvVars() {
        return taskEnvVars;
    }

    public InitTaskBuildItem withTaskEnvVars(Map<String, String> taskEnvVars) {
        return new InitTaskBuildItem(name, image, command, arguments, taskEnvVars, appEnvVars, sharedEnvironment,
                sharedFilesystem);
    }

    public Map<String, String> getAppEnvVars() {
        return appEnvVars;
    }

    public InitTaskBuildItem withAppEnvVars(Map<String, String> appEnvVars) {
        return new InitTaskBuildItem(name, image, command, arguments, taskEnvVars, appEnvVars, sharedEnvironment,
                sharedFilesystem);
    }

    public boolean isSharedEnvironment() {
        return sharedEnvironment;
    }

    public InitTaskBuildItem withSharedEnvironment(boolean sharedEnvironment) {
        return new InitTaskBuildItem(name, image, command, arguments, taskEnvVars, appEnvVars, sharedEnvironment,
                sharedFilesystem);
    }

    public boolean isSharedFilesystem() {
        return sharedFilesystem;
    }

    public InitTaskBuildItem withSharedFilesystem(boolean sharedFilesystem) {
        return new InitTaskBuildItem(name, image, command, arguments, taskEnvVars, appEnvVars, sharedEnvironment,
                sharedFilesystem);
    }
}
