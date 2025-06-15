package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PluginCatalog implements Catalog<PluginCatalog> {

    public static final String VERSION = "v1";
    protected static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final String version;
    private final String lastUpdate;
    private final Map<String, Plugin> plugins;

    @JsonIgnore
    private final Optional<Path> catalogLocation;

    public static PluginCatalog empty() {
        return new PluginCatalog();
    }

    public static PluginCatalog combine(Optional<PluginCatalog> userCatalog, Optional<PluginCatalog> projectCatalog) {
        Map<String, Plugin> plugins = new HashMap<>();
        plugins.putAll(userCatalog.map(PluginCatalog::getPlugins).orElse(Collections.emptyMap()));
        plugins.putAll(projectCatalog.map(PluginCatalog::getPlugins).orElse(Collections.emptyMap()));

        Optional<LocalDateTime> projectCatalogDate = projectCatalog.map(c -> c.getLastUpdateDate());
        Optional<LocalDateTime> userCatalogDate = projectCatalog.map(c -> c.getLastUpdateDate());

        LocalDateTime lastUpdated = Stream.of(projectCatalogDate, userCatalogDate)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now());

        return new PluginCatalog(VERSION, lastUpdated, plugins, Optional.empty());
    }

    public PluginCatalog() {
        this(Collections.emptyMap());
    }

    public PluginCatalog(Map<String, Plugin> plugins) {
        this(VERSION, now(), plugins, Optional.empty());
    }

    public PluginCatalog(String version, LocalDateTime lastUpdate, Map<String, Plugin> plugins,
            Optional<Path> catalogLocation) {
        this(version, DATETIME_FORMATTER.format(lastUpdate), plugins, catalogLocation);
    }

    public PluginCatalog(String version, String lastUpdate, Map<String, Plugin> plugins, Optional<Path> catalogLocation) {
        this.version = version;
        this.lastUpdate = lastUpdate;
        this.catalogLocation = catalogLocation;
        // Apply the the catalog location if available, else retain original values (needed for combined catalog).
        this.plugins = Collections.unmodifiableMap(plugins.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(),
                        e -> catalogLocation.isPresent() ? e.getValue().withCatalogLocation(catalogLocation) : e.getValue())));
    }

    public String getVersion() {
        return version;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    @JsonIgnore
    public LocalDateTime getLastUpdateDate() {
        return LocalDateTime.from(DATETIME_FORMATTER.parse(lastUpdate));
    }

    public Map<String, Plugin> getPlugins() {
        return plugins;
    }

    @Override
    public Optional<Path> getCatalogLocation() {
        return catalogLocation;
    }

    @Override
    public PluginCatalog withCatalogLocation(Optional<Path> catalogLocation) {
        return new PluginCatalog(version, lastUpdate, plugins, catalogLocation);
    }

    @Override
    public PluginCatalog refreshLastUpdate() {
        return new PluginCatalog(version, now(), plugins, catalogLocation);
    }

    public PluginCatalog addPlugin(Plugin plugin) {
        Map<String, Plugin> newPlugins = new HashMap<>(plugins);
        newPlugins.put(plugin.getName(), plugin);
        return new PluginCatalog(version, now(), newPlugins, catalogLocation);
    }

    public PluginCatalog removePlugin(Plugin plugin) {
        Map<String, Plugin> newPlugins = new HashMap<>(plugins);
        newPlugins.remove(plugin.getName());
        return new PluginCatalog(version, now(), newPlugins, catalogLocation);
    }

    public PluginCatalog removePlugin(String pluginName) {
        Map<String, Plugin> newPlugins = new HashMap<>(plugins);
        newPlugins.remove(pluginName);
        return new PluginCatalog(version, now(), newPlugins, catalogLocation);
    }

    private static String now() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }
}
