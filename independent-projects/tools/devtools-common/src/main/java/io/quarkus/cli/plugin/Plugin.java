package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class Plugin {

    private final String name;
    private final PluginType type;
    private final Optional<String> location;
    private final Optional<String> description;

    @JsonIgnore
    private final boolean inUserCatalog;
    /**
     * This is mostly used for testing.
     */
    @JsonIgnore
    private final Optional<Path> catalogLocation;

    public Plugin(String name, PluginType type) {
        this(name, type, Optional.empty(), Optional.empty(), Optional.empty(), true);
    }

    @JsonCreator
    public Plugin(@JsonProperty("name") String name,
            @JsonProperty("type") PluginType type,
            @JsonProperty("location") Optional<String> location,
            @JsonProperty("description") Optional<String> description) {
        this(name, type, location, description, Optional.empty(), true);
    }

    public Plugin(String name,
            PluginType type,
            Optional<String> location,
            Optional<String> description,
            Optional<Path> catalogLocation,
            boolean inUserCatalog) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.description = description != null ? description : Optional.empty();
        this.location = location != null ? location : Optional.empty();
        this.catalogLocation = catalogLocation != null ? catalogLocation : Optional.empty();
        this.inUserCatalog = inUserCatalog;
    }

    public String getName() {
        return name;
    }

    public PluginType getType() {
        return type;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public Optional<String> getLocation() {
        return location;
    }

    public boolean isInUserCatalog() {
        return inUserCatalog;
    }

    public boolean isInProjectCatalog() {
        return !inUserCatalog;
    }

    public Optional<Path> getCatalogLocation() {
        return catalogLocation;
    }

    public Plugin withName(String name) {
        return new Plugin(name, type, location, description, catalogLocation, inUserCatalog);
    }

    public Plugin withDescription(Optional<String> description) {
        return new Plugin(name, type, location, description, catalogLocation, inUserCatalog);
    }

    public Plugin withCatalogLocation(Optional<Path> catalogLocation) {
        return new Plugin(name, type, location, description, catalogLocation, inUserCatalog);
    }

    public Plugin withType(PluginType type) {
        return new Plugin(name, type, location, description, catalogLocation, inUserCatalog);
    }

    public Plugin inUserCatalog() {
        return new Plugin(name, type, location, description, catalogLocation, true);
    }

    public Plugin inProjectCatalog() {
        return new Plugin(name, type, location, description, catalogLocation, false);
    }
}
