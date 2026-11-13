package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JBangCatalog implements Catalog<JBangCatalog> {

    private final Map<String, JBangCatalog> catalogs;
    private final Map<String, JBangAlias> aliases;

    @JsonProperty("catalog-ref")
    private final Optional<String> catalogRef;

    @JsonIgnore
    private final Optional<Path> catalogLocation;

    public static JBangCatalog empty() {
        return new JBangCatalog();
    }

    public JBangCatalog() {
        this(Collections.emptyMap(), Collections.emptyMap(), Optional.empty(), Optional.empty());
    }

    public JBangCatalog(Map<String, JBangCatalog> catalogs, Map<String, JBangAlias> aliases, Optional<String> catalogRef,
            Optional<Path> catalogLocation) {
        this.catalogs = catalogs;
        this.aliases = aliases;
        this.catalogRef = catalogRef;
        this.catalogLocation = catalogLocation;
    }

    public Map<String, JBangCatalog> getCatalogs() {
        return catalogs;
    }

    public Map<String, JBangAlias> getAliases() {
        return aliases;
    }

    public Optional<String> getCatalogRef() {
        return catalogRef;
    }

    @Override
    public Optional<Path> getCatalogLocation() {
        return catalogLocation;
    }

    @Override
    public JBangCatalog refreshLastUpdate() {
        return this;
    }

    @Override
    public JBangCatalog withCatalogLocation(Optional<Path> catalogLocation) {
        return new JBangCatalog(catalogs, aliases, catalogRef, catalogLocation);
    }
}
