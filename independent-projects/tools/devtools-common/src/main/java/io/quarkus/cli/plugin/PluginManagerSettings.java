package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Settings class for the {@link PluginManager}.
 * The {@link PluginManager} can be used beyond the Quarkus CLI.
 * Users are able to build extensible CLI apps using the {@link PluginManager}
 * with customized settings.
 */
public class PluginManagerSettings {

    public static String DEFAULT_PLUGIN_PREFIX = "quarkus";
    public static String DEFAULT_REMOTE_JBANG_CATALOG = "quarkusio";
    public static Function<Path, Path> DEFAULT_RELATIVE_PATH_FUNC = p -> p.resolve(".quarkus").resolve("cli").resolve("plugins")
            .resolve("quarkus-cli-catalog.json");

    private final String pluginPrefix;
    private final String remoteJBangCatalog;
    private final Function<Path, Path> toRelativePath;

    public PluginManagerSettings(String pluginPrefix, String remoteJBangCatalog, Function<Path, Path> toRelativePath) {
        this.pluginPrefix = pluginPrefix;
        this.remoteJBangCatalog = remoteJBangCatalog;
        this.toRelativePath = toRelativePath;
    }

    public static PluginManagerSettings defaultSettings() {
        return new PluginManagerSettings(DEFAULT_PLUGIN_PREFIX, DEFAULT_REMOTE_JBANG_CATALOG, DEFAULT_RELATIVE_PATH_FUNC);
    }

    public static PluginManagerSettings create(String name, String remoteJbangCatalog) {
        return new PluginManagerSettings(name, remoteJbangCatalog,
                p -> p.resolve("." + name).resolve("cli").resolve("plugins").resolve(name + "-cli-catalog.json"));
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
     * The name of the JBang catalog to get plugins from.
     *
     * @return the name of the catalog.
     */
    public String getRemoteJBangCatalog() {
        return remoteJBangCatalog;
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
