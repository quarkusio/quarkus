package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.settings.SubCommandModeSettings;
import org.jboss.logging.Logger;

import io.quarkus.runtime.QuarkusApplication;

/**
 * Quarkus application runner that uses AeshConsoleRunner for interactive shell mode.
 * This provides a REPL (Read-Eval-Print Loop) where users can type multiple commands.
 */
@Dependent
public class CliRunner implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(CliRunner.class);

    private final CliCommandRegistryFactory registryFactory;
    private final CliConfig configuration;
    private final Instance<CliSettings> customizers;

    public CliRunner(CliCommandRegistryFactory registryFactory,
            CliConfig configuration,
            Instance<CliSettings> customizers) {
        this.registryFactory = registryFactory;
        this.configuration = configuration;
        this.customizers = customizers;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int run(String... args) throws Exception {
        try {
            var registryBuilder = registryFactory.create();

            var subCommandModeConfig = configuration.subCommandMode();
            SubCommandModeSettings subCommandModeSettings = SubCommandModeSettings.builder()
                    .enabled(subCommandModeConfig.enabled())
                    .exitCommand(subCommandModeConfig.exitCommand())
                    .alternativeExitCommand(
                            subCommandModeConfig.alternativeExitCommand().isEmpty() ? null
                                    : subCommandModeConfig.alternativeExitCommand())
                    .contextSeparator(subCommandModeConfig.contextSeparator())
                    .showContextOnEntry(subCommandModeConfig.showContextOnEntry())
                    .showArgumentInPrompt(subCommandModeConfig.showArgumentInPrompt())
                    .build();

            var settingsBuilder = CliSettingsHelper.createBaseSettings(configuration, customizers)
                    .persistHistory(configuration.persistHistory())
                    .historySize(configuration.historySize())
                    .subCommandModeSettings(subCommandModeSettings);

            if (configuration.historyFile().isPresent()) {
                settingsBuilder.historyFile(new java.io.File(configuration.historyFile().get()));
            }

            var settings = settingsBuilder.build();

            AeshConsoleRunner runner = AeshConsoleRunner.builder()
                    .commandRegistryBuilder(registryBuilder)
                    .settings(settings)
                    .prompt(configuration.prompt());

            if (configuration.addExitCommand()) {
                runner.addExitCommand();
            }

            runner.start();
            return 0;
        } catch (Exception e) {
            LOG.error("Error starting console", e);
            return 1;
        }
    }
}
