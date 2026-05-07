package io.quarkus.aesh.runtime;

import org.aesh.command.settings.SettingsBuilder;

/**
 * Shared helper for building aesh {@link SettingsBuilder} with common configuration.
 * Used by both {@link CliRunner} (local console) and
 * {@link AeshRemoteConnectionHandler} (remote sessions).
 */
final class CliSettingsHelper {

    private CliSettingsHelper() {
    }

    static SettingsBuilder createBaseSettings(CliConfig config, Iterable<CliSettings> customizers) {
        var settingsBuilder = SettingsBuilder.builder()
                .enableAlias(config.enableAlias())
                .enableExport(config.enableExport())
                .enableMan(config.enableMan())
                .logging(config.logging());

        for (CliSettings customizer : customizers) {
            customizer.customize(settingsBuilder);
        }

        return settingsBuilder;
    }
}
