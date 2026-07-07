package io.quarkus.gradle.application;

import javax.inject.Inject;

import org.gradle.build.event.BuildEventsListenerRegistry;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import io.quarkus.gradle.application.internal.plugin.PluginInternal;

/**
 * Standalone Gradle plugin for configuring, building, testing, running, and
 * deploying one or more named Quarkus application outputs.
 * <p>
 * Applying the plugin registers the {@value #EXTENSION_NAME} extension and
 * the fixed model, development, testing, and offline-preparation task
 * families. Named build tasks are registered from the extension's build
 * container. The plugin is designed for Gradle configuration cache and
 * Isolated Projects.
 */
public final class QuarkusApplicationPlugin extends PluginInternal {
    /**
     * Gradle plugin ID.
     */
    @SuppressWarnings("unused") // public facing information
    public static final String PLUGIN_ID = "io.quarkus.application";

    /**
     * Name of the root {@code QuarkusApplicationExtension}.
     */
    public static final String EXTENSION_NAME = "quarkusApplication";

    /**
     * Creates the plugin with Gradle's tooling-model and build-event services.
     * Gradle constructs plugin instances; direct construction is not a
     * supported application configuration mechanism.
     *
     * @param registry registry used for the plugin's Tooling API model
     * @param buildEventsListeners registry used by long-lived build sessions
     */
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public QuarkusApplicationPlugin(ToolingModelBuilderRegistry registry,
            BuildEventsListenerRegistry buildEventsListeners) {
        super(registry, buildEventsListeners);
    }
}
