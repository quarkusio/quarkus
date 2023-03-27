package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

public class PluginManager {

    private final MessageWriter output;
    private final PluginMangerState state;
    private final PluginManagerSettings settings;
    private final PluginManagerUtil util;

    public PluginManager(PluginManagerSettings settings, MessageWriter output, Optional<Path> userHome,
            Optional<Path> projectRoot, Optional<QuarkusProject> quarkusProject, Predicate<Plugin> pluginFilter) {
        this.settings = settings;
        this.output = output;
        this.util = PluginManagerUtil.getUtil(settings);
        this.state = new PluginMangerState(settings, output, userHome, projectRoot, quarkusProject, pluginFilter);
    }

    /**
     * Adds the {@link Plugin} with the specified name or location to the installed plugins.
     * Plugins that have been detected as installable may be added by name.
     * Remote plugins, that are not detected can be added by the location (e.g. url or maven coordinates).
     *
     * @param nameOrLocation The name or location of the plugin.
     * @return the pugin that was added wrapped in {@link Optional}, or empty if no plugin was added.
     */
    public Optional<Plugin> addPlugin(String nameOrLocation) {
        return addPlugin(nameOrLocation, Optional.empty());
    }

    /**
     * Adds the {@link Plugin} with the specified name or location to the installed plugins.
     * Plugins that have been detected as installable may be added by name.
     * Remote plugins, that are not detected can be added by the location (e.g. url or maven coordinates).
     *
     * @param nameOrLocation The name or location of the plugin.
     * @param description An optional description to add to the plugin.
     * @return The pugin that was added wrapped in {@link Optional}, or empty if no plugin was added.
     */
    public Optional<Plugin> addPlugin(String nameOrLocation, Optional<String> description) {
        PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
        String name = util.getName(nameOrLocation);
        if (PluginUtil.isRemoteLocation(nameOrLocation)) {
            Plugin plugin = new Plugin(name, PluginUtil.getType(nameOrLocation), Optional.of(nameOrLocation), description);
            PluginCatalog updatedCatalog = state.getPluginCatalog().addPlugin(plugin);
            pluginCatalogService.writeCatalog(updatedCatalog);
            return Optional.of(plugin);
        }

        Map<String, Plugin> installablePlugins = state.installablePlugins();
        Optional<Plugin> plugin = Optional.ofNullable(installablePlugins.get(name));
        return plugin.map(p -> {
            PluginCatalog updatedCatalog = state.getPluginCatalog().addPlugin(p);
            pluginCatalogService.writeCatalog(updatedCatalog);
            return p;
        });
    }

    /**
     * Adds the {@link Plugin} with the specified name or location to the installed plugins.
     * Plugins that have been detected as installable may be added by name.
     * Remote plugins, that are not detected can be added by the location (e.g. url or maven coordinates).
     *
     * @param plugin The plugin.
     * @return The pugin that was added wrapped in {@link Optional}, or empty if no plugin was added.
     */
    public Optional<Plugin> addPlugin(Plugin plugin) {
        PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
        PluginCatalog updatedCatalog = state.getPluginCatalog().addPlugin(plugin);
        pluginCatalogService.writeCatalog(updatedCatalog);
        return Optional.of(plugin);
    }

    /**
     * Removes a {@link Plugin} by name.
     * The catalog from which the plugin will be removed is selected
     * based on where the plugin is found. If plugin is found in both catalogs
     * the project catalog is prefered.
     *
     * @param name The name of the plugin to remove.
     * @return The removed plugin wrapped in Optional, empty if no plugin was removed.
     */
    public Optional<Plugin> removePlugin(String name) {
        PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
        Plugin plugin = state.getInstalledPluigns().get(name);
        if (plugin == null) {
            return Optional.empty();
        } else if (state.getProjectCatalog().map(PluginCatalog::getPlugins).map(p -> p.containsKey(name)).orElse(false)) {
            pluginCatalogService.writeCatalog(state.getProjectCatalog()
                    .orElseThrow(() -> new IllegalStateException("Project catalog should be available!"))
                    .removePlugin(name));
            return Optional.of(plugin);
        }

        pluginCatalogService.writeCatalog(state.getUserCatalog()
                .orElseThrow(() -> new IllegalStateException("User catalog should be available!"))
                .removePlugin(name));
        return Optional.of(plugin);
    }

    /**
     * Removes a {@link Plugin} by name.
     * The catalog from which the plugin will be removed is selected
     * based on where the plugin is found. If plugin is found in both catalogs
     * the project catalog is prefered.
     *
     * @param plugin The plugin to remove
     * @return The removed plugin wrapped in Optional, empty if no plugin was removed.
     */
    public Optional<Plugin> removePlugin(Plugin plugin) {
        return removePlugin(plugin.getName());
    }

    /**
     * Check that the installed plugins are still available in the environment.
     *
     * @return true if any catalog was changed.
     */
    public boolean reconcile() {
        //We are using `|` instead of `||` cause we always want both branches to be executed
        if (state.getUserCatalog().map(c -> reconcile(c)).orElse(false)
                | state.getProjectCatalog().map(c -> reconcile(c)).orElse(false)) {
            // Refresh the list of installed plugins
            state.invalidate();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check that the installed plugins are still available in the environment.
     *
     * @param catalog The {@PluginCatalog} to use
     * @return true if catalog was modified
     */
    private boolean reconcile(PluginCatalog catalog) {
        Path location = catalog.getCatalogLocation()
                .orElseThrow(() -> new IllegalArgumentException("Unknwon plugin catalog location."));
        List<PluginType> installedTypes = catalog.getPlugins().entrySet().stream().map(Map.Entry::getValue).map(Plugin::getType)
                .collect(Collectors.toList());
        Map<String, Plugin> installablePlugins = state.installablePlugins(installedTypes);

        Map<String, Plugin> unreachable = catalog.getPlugins().entrySet().stream()
                .filter(i -> !installablePlugins.containsKey(i.getKey()))
                .filter(i -> PluginUtil.shouldRemove(i.getValue()))
                .collect(Collectors.toMap(m -> m.getKey(), m -> m.getValue()));

        if (unreachable.isEmpty()) {
            return false;
        }

        Path backupLocation = location.getParent().resolve("quarkus-cli-catalog.json.bkp");

        output.warn(
                "The following plugins found in the catalog: [%s] but no longer available: %s.\n"
                        + "The unavailable plugin will be purged! A backup of the catalog will be saved at: [%s].",
                location,
                unreachable.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.joining(", ", "[", "]")),
                backupLocation);

        PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
        pluginCatalogService.writeCatalog(catalog.withCatalogLocation(Optional.of(backupLocation)));
        for (String u : unreachable.keySet()) {
            catalog = catalog.removePlugin(u);
        }
        pluginCatalogService.writeCatalog(catalog);
        return true;
    }

    /**
     * Remove unavailable plugins, add extension plugins if available.
     *
     * @return true if changes any catalog was modified.
     */
    public boolean sync() {
        boolean catalogModified = reconcile();
        Map<String, Plugin> installedPlugins = getInstalledPlugins();
        Map<String, Plugin> extensionPlugins = state.getExtensionPlugins();
        Map<String, Plugin> pluginsToInstall = extensionPlugins.entrySet().stream()
                .filter(e -> !installedPlugins.containsKey(e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        catalogModified = catalogModified || !pluginsToInstall.isEmpty();
        pluginsToInstall.forEach((name, plugin) -> {
            addPlugin(plugin);
        });
        state.invalidate();
        if (!catalogModified) {
            PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
            PluginCatalog catalog = state.getPluginCatalog();
            pluginCatalogService.writeCatalog(catalog);
        }
        return catalogModified;
    }

    /**
     * Optionally sync if needed.
     * Sync happens weekly or when project files are updated.
     */
    public boolean syncIfNeeded() {
        if (!settings.isInteractiveMode()) {
            //syncing may require user interaction, so just return false
            return false;
        }
        PluginCatalog catalog = state.getCombinedCatalog();
        if (PluginUtil.shouldSync(state.getProjectRoot(), catalog)) {
            output.info("Plugin catalog last updated on: " + catalog.getLastUpdate() + ". Syncing!");
            return sync();
        }
        return false;
    }

    public Map<String, Plugin> getInstalledPlugins() {
        return state.getInstalledPluigns();
    }

    public Map<String, Plugin> getInstallablePlugins() {
        return state.getInstallablePlugins();
    }
}
