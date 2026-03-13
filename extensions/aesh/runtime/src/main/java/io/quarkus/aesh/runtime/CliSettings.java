package io.quarkus.aesh.runtime;

import org.aesh.command.settings.SettingsBuilder;

/**
 * Interface for customizing CLI settings.
 * <p>
 * Implement this interface as a CDI bean to customize the {@link SettingsBuilder}
 * before the console or runtime runner is created. This allows you to configure
 * advanced aesh features not exposed via Quarkus configuration properties.
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;ApplicationScoped
 * public class MyCliSettings implements CliSettings {
 *
 *     &#64;Override
 *     public void customize(SettingsBuilder&lt;?, ?, ?, ?, ?, ?&gt; builder) {
 *         builder.enableAlias(true)
 *                 .enableMan(true)
 *                 .persistHistory(true)
 *                 .historyFile(new File(".myapp_history"))
 *                 .logging(true);
 *     }
 * }
 * </pre>
 * <p>
 * Multiple customizers can be registered. They are applied in arbitrary order,
 * so avoid conflicting configurations across customizers.
 *
 * @see org.aesh.command.settings.SettingsBuilder
 */
public interface CliSettings {

    /**
     * Customize the SettingsBuilder before the console is started.
     * <p>
     * The builder is pre-configured with:
     * <ul>
     * <li>Command registry (from discovered commands)</li>
     * <li>Sub-command mode settings (from Quarkus configuration)</li>
     * <li>Default values for alias, export, man, history (all disabled)</li>
     * </ul>
     * <p>
     * You can override any of these settings or add additional configuration.
     *
     * @param builder the SettingsBuilder to customize
     */
    void customize(SettingsBuilder<?, ?, ?, ?, ?, ?> builder);
}
