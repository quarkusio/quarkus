package io.quarkus.cli.plugin;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

public class PluginManager {

    private static PluginManager INSTANCE;

    private final MessageWriter output;
    private final PluginMangerState state;
    private final PluginManagerSettings settings;
    private final PluginManagerUtil util;

    public synchronized static PluginManager get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("No instance of PluginManager found");
        }
        return INSTANCE;
    }

    public synchronized static PluginManager create(PluginManagerSettings settings, MessageWriter output,
            Optional<Path> userHome, Optional<Path> currentDir, Supplier<QuarkusProject> quarkusProject) {
        if (INSTANCE == null) {
            INSTANCE = new PluginManager(settings, output, userHome, currentDir, quarkusProject);
        }
        return INSTANCE;
    }

    PluginManager(PluginManagerSettings settings, MessageWriter output, Optional<Path> userHome,
            Optional<Path> currentDir, Supplier<QuarkusProject> quarkusProject) {
        this.settings = settings;
        this.output = output;
        this.util = PluginManagerUtil.getUtil(settings);
        this.state = new PluginMangerState(settings, output, userHome, currentDir, quarkusProject);
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
        return addPlugin(nameOrLocation, false, Optional.empty());
    }

    /**
     * Adds the {@link Plugin} with the specified name or location to the installed plugins.
     * Plugins that have been detected as installable may be added by name.
     * Remote plugins, that are not detected can be added by the location (e.g. url or maven coordinates).
     *
     * @param nameOrLocation The name or location of the plugin.
     * @param userCatalog Flag to only use the user catalog.
     * @param description An optional description to add to the plugin.
     * @return The pugin that was added wrapped in {@link Optional}, or empty if no plugin was added.
     */
    public Optional<Plugin> addPlugin(String nameOrLocation, boolean userCatalog, Optional<String> description) {
        PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
        String name = util.getName(nameOrLocation);
        Optional<String> location = Optional.empty();

        if (PluginUtil.isRemoteLocation(nameOrLocation)) {
            location = Optional.of(nameOrLocation);
        } else if (PluginUtil.isLocalFile(nameOrLocation)) {

            Optional<Path> projectRelative = state.getProjectRoot()
                    .filter(r -> !userCatalog) // If users catalog selected ignore project relative paths.
                    .filter(r -> PluginUtil.isProjectFile(r, nameOrLocation)) // check if its project file
                    .map(r -> r.relativize(Path.of(nameOrLocation).toAbsolutePath()));

            location = projectRelative
                    .or(() -> Optional.of(nameOrLocation).map(Path::of).map(Path::toAbsolutePath))
                    .map(Path::toString);
        }

        if (!location.isEmpty()) {
            Plugin plugin = new Plugin(name, PluginUtil.getType(nameOrLocation), location, description, Optional.empty(),
                    userCatalog || state.getProjectCatalog().isEmpty());
            PluginCatalog updatedCatalog = state.pluginCatalog(userCatalog).addPlugin(plugin);
            pluginCatalogService.writeCatalog(updatedCatalog);
            state.invalidateInstalledPlugins();
            return Optional.of(plugin);
        }

        Map<String, Plugin> installablePlugins = state.installablePlugins();
        Optional<Plugin> plugin = Optional.ofNullable(installablePlugins.get(name)).map(Plugin::inUserCatalog);
        return plugin.map(p -> {
            Plugin withDescription = p.withDescription(description);
            PluginCatalog updatedCatalog = state.pluginCatalog(userCatalog).addPlugin(withDescription);
            pluginCatalogService.writeCatalog(updatedCatalog);
            state.invalidateInstalledPlugins();
            return withDescription;
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
        return addPlugin(plugin, false);
    }

    /**
     * Adds the {@link Plugin} with the specified name or location to the installed plugins.
     * Plugins that have been detected as installable may be added by name.
     * Remote plugins, that are not detected can be added by the location (e.g. url or maven coordinates).
     *
     * @param plugin The plugin.
     * @param userCatalog Flag to only use the user catalog.
     * @return The pugin that was added wrapped in {@link Optional}, or empty if no plugin was added.
     */
    public Optional<Plugin> addPlugin(Plugin plugin, boolean userCatalog) {
        PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
        PluginCatalog updatedCatalog = state.pluginCatalog(userCatalog).addPlugin(plugin);
        pluginCatalogService.writeCatalog(updatedCatalog);
        state.invalidateInstalledPlugins();
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
        return removePlugin(name, false);
    }

    /**
     * Removes a {@link Plugin} by name.
     * The catalog from which the plugin will be removed is selected
     * based on where the plugin is found. If plugin is found in both catalogs
     * the project catalog is prefered.
     *
     * @param name The name of the plugin to remove.
     * @param userCatalog Flag to only use the user catalog.
     * @return The removed plugin wrapped in Optional, empty if no plugin was removed.
     */
    public Optional<Plugin> removePlugin(String name, boolean userCatalog) {
        PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
        Plugin plugin = state.getInstalledPluigns().get(name);
        if (plugin == null) {
            return Optional.empty();
        } else if (userCatalog) {
            Optional<Plugin> userPlugin = state.getUserCatalog().map(PluginCatalog::getPlugins).map(p -> p.get(name));
            return userPlugin.map(p -> {
                pluginCatalogService.writeCatalog(
                        state.getUserCatalog().orElseThrow(() -> new IllegalStateException("User catalog should be available"))
                                .removePlugin(p));
                state.invalidateInstalledPlugins();
                return p;
            });
        }

        if (plugin.isInUserCatalog()) {
            pluginCatalogService.writeCatalog(state.getUserCatalog()
                    .orElseThrow(() -> new IllegalStateException("User catalog should be available")).removePlugin(plugin));
        } else {
            pluginCatalogService.writeCatalog(state.getProjectCatalog()
                    .orElseThrow(() -> new IllegalStateException("Project catalog should be available")).removePlugin(plugin));
        }
        state.invalidateInstalledPlugins();
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
        return removePlugin(plugin, false);
    }

    /**
     * Removes a {@link Plugin} by name.
     * The catalog from which the plugin will be removed is selected
     * based on where the plugin is found. If plugin is found in both catalogs
     * the project catalog is prefered.
     *
     * @param plugin The plugin to remove
     * @param userCatalog Flag to only use the user catalog.
     * @return The removed plugin wrapped in Optional, empty if no plugin was removed.
     */
    public Optional<Plugin> removePlugin(Plugin plugin, boolean userCatalog) {
        return removePlugin(plugin.getName(), userCatalog);
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
                .orElseThrow(() -> new IllegalArgumentException("Unknown plugin catalog location."));
        List<PluginType> installedTypes = catalog.getPlugins().entrySet().stream().map(Map.Entry::getValue).map(Plugin::getType)
                .collect(Collectors.toList());
        //Let's only fetch installable plugins of the corresponding types.
        //This will help us avoid uneeded calls to things like jbang if no jbang plugins are installed
        Map<String, Plugin> installablePlugins = state.installablePlugins(installedTypes).entrySet().stream()
                .filter(e -> installedTypes.contains(e.getValue().getType()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        Map<String, Plugin> unreachable = catalog.getPlugins().entrySet().stream()
                .filter(i -> !installablePlugins.containsKey(i.getKey()))
                .filter(i -> PluginUtil.shouldRemove(i.getValue()))
                .collect(Collectors.toMap(m -> m.getKey(), m -> m.getValue()));

        if (unreachable.isEmpty()) {
            return false;
        }

        Path backupLocation = location.getParent().resolve("quarkus-cli-catalog.json.bkp");

        output.warn(
                "The following plugins were found in the catalog: [%s] but are no longer available: %s.\n"
                        + "The unavailable plugins will be purged. A backup of the catalog will be saved at: [%s].",
                location,
                unreachable.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.joining(", ", "[", "]")),
                backupLocation);

        PluginCatalogService pluginCatalogService = state.getPluginCatalogService();
        pluginCatalogService.writeCatalog(catalog.withCatalogLocation(Optional.of(backupLocation)));
        for (String u : unreachable.keySet()) {
            catalog = catalog.removePlugin(u);
        }
        pluginCatalogService.writeCatalog(catalog);
        // here we are just touching the catalog, no need to invalidate
        return true;
    }

    /**
     * Remove unavailable plugins, add extension plugins if available.
     *
     * @return true if changes any catalog was modified.
     */
    public boolean sync() {
        if (state.isSynced()) {
            return false;
        }
        try {
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
                PluginCatalog catalog = state.pluginCatalog(false);
                pluginCatalogService.writeCatalog(catalog);
                // here we are just touching the catalog, no need to invalidate
            }
            return catalogModified;
        } finally {
            state.synced();
        }
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

        // Check if there project catalog file is missing
        boolean createdMissingProjectCatalog = state.getPluginCatalogService().findProjectCatalogPath(state.getProjectRoot())
                .map(Path::toFile)
                .filter(Predicate.not(File::exists))
                .map(File::toPath)
                .map(p -> {
                    output.debug("Project plugin catalog has not been initialized. Initializing.");
                    state.getPluginCatalogService().writeCatalog(new PluginCatalog().withCatalogLocation(p));
                    return true;
                }).orElse(false);

        if (createdMissingProjectCatalog) {
            return sync();
        }

        PluginCatalog catalog = state.getCombinedCatalog();
        if (PluginUtil.shouldSync(state.getProjectRoot(), catalog)) {
            output.debug("Plugin catalog last updated on: " + catalog.getLastUpdate() + ". Syncing.");
            return sync();
        }
        return false;
    }

    public Map<String, Plugin> getInstalledPlugins(boolean userCatalog) {
        return userCatalog ? state.userPlugins() : state.getInstalledPluigns();
    }

    public Map<String, Plugin> getInstalledPlugins() {
        return getInstalledPlugins(false);
    }

    public Map<String, Plugin> getInstallablePlugins() {
        return state.getInstallablePlugins();
    }

    public PluginManagerUtil getUtil() {
        return util;
    }
}
