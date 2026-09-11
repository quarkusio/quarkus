package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.Project;

final class TestOwnership {

    private static final String LEGACY_PLUGIN_ID = "io.quarkus";

    private final boolean legacyPluginPresent;

    TestOwnership(Project project) {
        legacyPluginPresent = project.getPlugins().hasPlugin(LEGACY_PLUGIN_ID);
    }

    boolean ownedByLegacyPlugin() {
        return legacyPluginPresent;
    }
}
