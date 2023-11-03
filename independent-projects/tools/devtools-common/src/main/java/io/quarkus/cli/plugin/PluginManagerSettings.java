package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

/**
 * Settings class for the {@link PluginManager}.
 * The {@link PluginManager} can be used beyond the Quarkus CLI.
 * Users are able to build extensible CLI apps using the {@link PluginManager}
 * with customized settings.
 */
public class PluginManagerSettings {

    public static String DEFAULT_PLUGIN_PREFIX = "quarkus";
    public static String FALLBACK_REMOTE_JBANG_CATALOG = "quarkusio";
    public static String[] DEFAULT_REMOTE_JBANG_CATALOGS = new String[0];
    public static Function<Path, Path> DEFAULT_RELATIVE_PATH_FUNC = p -> p.resolve(".quarkus").resolve("cli").resolve("plugins")
            .resolve("quarkus-cli-catalog.json");

    private final boolean interactiveMode;
    private final String pluginPrefix;
    private final String fallbackJBangCatalog;
    private final String[] remoteJBangCatalogs;
    private final Function<Path, Path> toRelativePath;

    public PluginManagerSettings(boolean interactiveMode, String pluginPrefix, String fallbackJBangCatalog,
            String[] remoteJBangCatalogs,
            Function<Path, Path> toRelativePath) {
        this.interactiveMode = interactiveMode;
        this.pluginPrefix = pluginPrefix;
        this.fallbackJBangCatalog = fallbackJBangCatalog;
        this.remoteJBangCatalogs = remoteJBangCatalogs;
        this.toRelativePath = toRelativePath;
    }

    public static PluginManagerSettings defaultSettings() {
        return new PluginManagerSettings(false, DEFAULT_PLUGIN_PREFIX, FALLBACK_REMOTE_JBANG_CATALOG,
                DEFAULT_REMOTE_JBANG_CATALOGS,
                DEFAULT_RELATIVE_PATH_FUNC);
    }

    public PluginManagerSettings withPluignPrefix(String pluginPrefix) {
        return new PluginManagerSettings(interactiveMode, pluginPrefix, fallbackJBangCatalog, remoteJBangCatalogs,
                toRelativePath);
    }

    public PluginManagerSettings withCatalogs(Set<String> remoteJBangCatalogs) {
        return new PluginManagerSettings(interactiveMode, pluginPrefix, fallbackJBangCatalog,
                remoteJBangCatalogs.toArray(new String[0]), toRelativePath);
    }

    public PluginManagerSettings withCatalogs(String... remoteJBangCatalogs) {
        return new PluginManagerSettings(interactiveMode, pluginPrefix, fallbackJBangCatalog, remoteJBangCatalogs,
                toRelativePath);
    }

    public PluginManagerSettings withInteractivetMode(boolean interactiveMode) {
        return new PluginManagerSettings(interactiveMode, pluginPrefix, fallbackJBangCatalog, remoteJBangCatalogs,
                toRelativePath);
    }

    /**
     * The prefix of the {@link Plugin}.
     * This value is used to strip the prefix for the location
     * when creating the name.
     *
     * @return the prefix.
     */
    public String getPluginPrefix() {
        return pluginPrefix;
    }

    /**
     * The name of the fallback JBang catalogs to get plugins from.
     *
     * @return the name of the catalog.
     */
    public String getFallbackJBangCatalog() {
        return fallbackJBangCatalog;
    }

    /**
     * The names of the JBang catalogs to get plugins from.
     *
     * @return the name of the catalog.
     */
    public String[] getRemoteJBangCatalogs() {
        return remoteJBangCatalogs;
    }

    public boolean isInteractiveMode() {
        return interactiveMode;
    }

    /**
     * A {@link Function} from getting the relative path to the catalog.
     * For example: `~ -> ~/.quarkus/cli/plugins/quarkus-cli-catalog.json`.
     *
     * @return the path.
     */
    public Function<Path, Path> getToRelativePath() {
        return toRelativePath;
    }
}
