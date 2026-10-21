package io.quarkus.cli.plugin;

import static io.quarkus.cli.plugin.PluginManagerUtil.ALIAS_SEPARATOR;
import static io.quarkus.cli.plugin.PluginManagerUtil.getTransitives;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.platform.catalog.processor.ExtensionProcessor;
import io.quarkus.registry.catalog.Extension;

class PluginMangerState {

    PluginMangerState(PluginManagerSettings settings, MessageWriter output, Optional<Path> userHome, Optional<Path> currentDir,
            Supplier<QuarkusProject> quarkusProject) {
        this.settings = settings;
        this.output = output;
        this.userHome = userHome;
        this.quarkusProject = quarkusProject;

        //Inferred
        this.jbangCatalogService = new JBangCatalogService(settings.isInteractiveMode(), output, settings.getPluginPrefix(),
                settings.getFallbackJBangCatalog(),
                settings.getRemoteJBangCatalogs());
        this.pluginCatalogService = new PluginCatalogService(settings.getToRelativePath());
        this.projectRoot = currentDir.flatMap(CatalogService::findProjectRoot);
        this.util = PluginManagerUtil.getUtil(settings);
    }

    private final PluginManagerSettings settings;
    private final MessageWriter output;
    private final PluginManagerUtil util;
    private final Optional<Path> userHome;
    private final Optional<Path> projectRoot;
    private final Supplier<QuarkusProject> quarkusProject;

    private final PluginCatalogService pluginCatalogService;
    private final JBangCatalogService jbangCatalogService;

    //
    private Map<String, Plugin> _userPlugins;
    private Map<String, Plugin> _projectPlugins;
    private Map<String, Plugin> _installedPlugins;

    private Map<String, Plugin> _installablePlugins;
    private Map<String, Plugin> _extensionPlugins;

    private Optional<PluginCatalog> _userCatalog;
    private Optional<PluginCatalog> _projectCatalog;

    private PluginCatalog _combinedCatalog;

    private boolean synced;

    public PluginCatalogService getPluginCatalogService() {
        return pluginCatalogService;
    }

    public JBangCatalogService getJbangCatalogService() {
        return jbangCatalogService;
    }

    public Map<String, Plugin> installedPlugins() {
        Map<String, Plugin> allInstalledPlugins = new HashMap<>();
        allInstalledPlugins.putAll(userPlugins());
        allInstalledPlugins.putAll(projectPlugins());
        return allInstalledPlugins;
    }

    public Map<String, Plugin> getInstalledPluigns() {
        if (_installedPlugins == null) {
            _installedPlugins = installedPlugins();
        }
        return Collections.unmodifiableMap(_installedPlugins);
    }

    public Map<String, Plugin> projectPlugins() {
        return pluginCatalogService.readProjectCatalog(projectRoot).map(catalog -> catalog.getPlugins().values().stream()
                .map(Plugin::inProjectCatalog)
                .collect(Collectors.toMap(p -> p.getName(), p -> p))).orElse(Collections.emptyMap());
    }

    public Map<String, Plugin> getProjectPluigns() {
        if (_projectPlugins == null) {
            _projectPlugins = projectPlugins();
        }
        return Collections.unmodifiableMap(_projectPlugins);
    }

    public Map<String, Plugin> userPlugins() {
        return pluginCatalogService.readUserCatalog(userHome).map(catalog -> catalog.getPlugins().values().stream()
                .map(Plugin::inUserCatalog)
                .collect(Collectors.toMap(p -> p.getName(), p -> p))).orElse(Collections.emptyMap());
    }

    public Map<String, Plugin> getUserPluigns() {
        if (_userPlugins == null) {
            _userPlugins = userPlugins();
        }
        return Collections.unmodifiableMap(_userPlugins);
    }

    public Map<String, Plugin> installablePlugins(List<PluginType> types) {
        Map<String, Plugin> installablePlugins = new HashMap<>();
        for (PluginType type : types) {
            switch (type) {
                case jbang:
                    installablePlugins.putAll(jbangPlugins());
                    break;
                case executable:
                    installablePlugins.putAll(executablePlugins());
                    break;
                case extension:
                    installablePlugins.putAll(extensionPlugins());
                    break;
            }
        }
        installablePlugins.putAll(executablePlugins().entrySet().stream().filter(e -> types.contains(e.getValue().getType()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        return installablePlugins;
    }

    public Map<String, Plugin> installablePlugins(PluginType... types) {
        return installablePlugins(List.of(types));
    }

    public Map<String, Plugin> installablePlugins() {
        return installablePlugins(PluginType.values());
    }

    public Map<String, Plugin> getInstallablePlugins() {
        if (_installablePlugins == null) {
            this._installablePlugins = installablePlugins();
        }
        return Collections.unmodifiableMap(_installablePlugins);
    }

    public Map<String, Plugin> jbangPlugins() {
        boolean isUserScoped = !projectRoot.isPresent();
        Map<String, Plugin> jbangPlugins = new HashMap<>();
        jbangCatalogService.ensureJBangIsInstalled();
        JBangCatalog jbangCatalog = jbangCatalogService.readCombinedCatalog(projectRoot, userHome);
        jbangCatalog.getAliases().forEach((location, alias) -> {
            String name = util.getName(location);
            Optional<String> description = alias.getDescription();
            Plugin plugin = new Plugin(name, PluginType.jbang, Optional.of(location), description, Optional.empty(),
                    isUserScoped);
            jbangPlugins.put(name, plugin);
        });
        return jbangPlugins;
    }

    public Map<String, Plugin> executablePlugins() {
        boolean isUserScoped = !projectRoot.isPresent();
        Map<String, Plugin> executablePlugins = new HashMap<>();
        Binaries.findQuarkusPrefixedCommands().forEach(f -> {
            String name = util.getName(f.getName());
            Optional<String> description = Optional.empty();
            Optional<String> location = Optional.of(f.getAbsolutePath());
            Plugin plugin = new Plugin(name, PluginType.executable, location, description, Optional.empty(), isUserScoped);
            executablePlugins.put(name, plugin);
        });
        return executablePlugins;
    }

    public Map<String, Plugin> extensionPlugins() {
        //Get extension plugins
        Map<String, Plugin> extensionPlugins = new HashMap<>();
        projectRoot.map(r -> quarkusProject.get()).ifPresent(project -> {
            try {
                Map<ArtifactKey, Extension> allExtensions = new HashMap<>();
                project.getExtensionsCatalog().getExtensions().forEach(e -> allExtensions.put(e.getArtifact().getKey(), e));

                for (ArtifactCoords artifactCoords : project.getExtensionManager().getInstalled()) {
                    ArtifactKey artifactKey = artifactCoords.getKey();
                    List<ArtifactKey> allKeys = new ArrayList<>();
                    allKeys.add(artifactKey);
                    allKeys.addAll(getTransitives(artifactKey, allExtensions));

                    for (ArtifactKey key : allKeys) {
                        Extension extension = allExtensions.get(key);
                        for (String cliPlugin : ExtensionProcessor.getCliPlugins(extension)) {
                            Plugin plugin = (cliPlugin.contains(ALIAS_SEPARATOR) ? util.fromAlias(cliPlugin)
                                    : util.fromLocation(cliPlugin)).withType(PluginType.extension);
                            extensionPlugins.put(plugin.getName(), plugin);
                        }
                    }
                }
            } catch (Exception ignore) {
                output.warn("Failed to read the extension catalog. Ignoring extension plugins.");
            }
        });
        return extensionPlugins;
    }

    public Map<String, Plugin> getExtensionPlugins() {
        if (_extensionPlugins == null) {
            this._extensionPlugins = extensionPlugins();
        }

        return Collections.unmodifiableMap(_extensionPlugins);
    }

    public Optional<PluginCatalog> projectCatalog() {
        return projectRoot.flatMap(p -> pluginCatalogService.readProjectCatalog(Optional.of(p)));
    }

    public Optional<PluginCatalog> getProjectCatalog() {
        if (_projectCatalog == null) {
            _projectCatalog = pluginCatalogService.readProjectCatalog(projectRoot);
        }
        return _projectCatalog;
    }

    public Optional<PluginCatalog> userCatalog() {
        return userHome.flatMap(h -> pluginCatalogService.readUserCatalog(Optional.of(h)));
    }

    public Optional<PluginCatalog> getUserCatalog() {
        if (_userCatalog == null) {
            _userCatalog = userCatalog();
        }
        return _userCatalog;
    }

    public PluginCatalog combinedCatalog() {
        return PluginCatalog.combine(getUserCatalog(), getProjectCatalog());
    }

    public PluginCatalog getCombinedCatalog() {
        if (_combinedCatalog == null) {
            _combinedCatalog = combinedCatalog();
        }
        return _combinedCatalog;
    }

    public PluginCatalog pluginCatalog(boolean userCatalog) {
        return (userCatalog ? getUserCatalog() : getProjectCatalog()).or(() -> getUserCatalog())
                .orElseThrow(() -> new IllegalStateException("Unable to get project and user plugin catalogs!"));
    }

    public Optional<Path> getProjectRoot() {
        return this.projectRoot;
    }

    public boolean isSynced() {
        return synced;
    }

    public void synced() {
        this.synced = true;
    }

    public void invalidateCatalogs() {
        _projectCatalog = null;
        _userCatalog = null;
        _combinedCatalog = null;
    }

    public void invalidateInstalledPlugins() {
        _userPlugins = null;
        _projectPlugins = null;
        _installedPlugins = null;
        invalidateCatalogs();
    }

    public void invalidate() {
        _userPlugins = null;
        _projectPlugins = null;
        _installedPlugins = null;
        _installablePlugins = null;
        _extensionPlugins = null;
    }
}
