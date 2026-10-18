package io.quarkus.aesh.runtime;

import java.io.File;

import org.aesh.command.settings.SettingsBuilder;

/**
 * Shared helper for building aesh {@link SettingsBuilder} with common configuration.
 * Used by {@link CliRunner} (local console), {@link AeshRemoteConnectionHandler}
 * (remote sessions), and indirectly by {@code DefaultAeshRuntimeRunnerFactory}
 * (runtime mode).
 * <p>
 * Centralizes the mapping from {@link CliConfig} properties to
 * {@link SettingsBuilder} so all execution paths share a consistent
 * baseline configuration.
 */
final class CliSettingsHelper {

    private CliSettingsHelper() {
    }

    static SettingsBuilder createBaseSettings(CliConfig config, Iterable<CliSettings> customizers) {
        var settingsBuilder = SettingsBuilder.builder()
                .enableAlias(config.enableAlias())
                .enableExport(config.enableExport())
                .enableMan(config.enableMan())
                .logging(config.logging())
                .persistHistory(config.persistHistory())
                .historySize(config.historySize());

        if (config.historyFile().isPresent()) {
            settingsBuilder.historyFile(new File(config.historyFile().get()));
        }

        for (CliSettings customizer : customizers) {
            customizer.customize(settingsBuilder);
        }

        return settingsBuilder;
    }
}
