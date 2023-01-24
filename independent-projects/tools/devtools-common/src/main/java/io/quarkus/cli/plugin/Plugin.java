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

    /**
     * This is mostly used for testing.
     */
    @JsonIgnore
    private final Optional<Path> catalogLocation;

    public Plugin(String name, PluginType type) {
        this(name, type, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Plugin(String name, PluginType type, Optional<String> location, Optional<String> description) {
        this(name, type, location, description, Optional.empty());
    }

    @JsonCreator
    public Plugin(@JsonProperty("name") String name,
            @JsonProperty("type") PluginType type,
            @JsonProperty("location") Optional<String> location,
            @JsonProperty("description") Optional<String> description,
            @JsonProperty("catalogLocation") Optional<Path> catalogLocation) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.description = description != null ? description : Optional.empty();
        this.location = location != null ? location : Optional.empty();
        this.catalogLocation = catalogLocation != null ? catalogLocation : Optional.empty();
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

    public Optional<Path> getCatalogLocation() {
        return catalogLocation;
    }

    public Plugin withDescription(Optional<String> description) {
        return new Plugin(name, type, location, description, catalogLocation);
    }

    public Plugin withCatalogLocation(Optional<Path> catalogLocation) {
        return new Plugin(name, type, location, description, catalogLocation);
    }
}
