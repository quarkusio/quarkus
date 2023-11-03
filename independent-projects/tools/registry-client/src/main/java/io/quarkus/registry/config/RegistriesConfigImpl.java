package io.quarkus.registry.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.quarkus.registry.json.JsonBuilder;

/**
 * Top of the config hierarchy. Holder of the rest of the things.
 *
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see RegistriesConfig#builder() creates a builder
 * @see RegistriesConfig#mutable() creates a builder from an existing RegistriesConfig
 * @see RegistriesConfig#mutableFromFile(Path) creates a builder from the contents of a file
 * @see RegistriesConfig#fromFile(Path) creates (and builds) a builder from the contents of a file
 * @see RegistriesConfig#persist() to save an updated file-based configuration
 * @see RegistriesConfig#persist(Path) for writing an RegistriesConfig to a file
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonSerialize(using = RegistriesConfigImpl.Serializer.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistriesConfigImpl implements RegistriesConfig {
    private final boolean debug;
    private final List<RegistryConfig> registries;
    private ConfigSource configSource;

    private RegistriesConfigImpl(Builder builder) {
        this.debug = builder.debug;
        this.configSource = builder.configSource;

        if (builder.registries.isEmpty()) {
            this.registries = Collections.singletonList(RegistryConfigImpl.getDefaultRegistry());
        } else {
            List<RegistryConfig> builtConfig = new ArrayList<>(builder.registries.size());
            boolean sawEnabled = false;
            for (RegistryConfig r : builder.registries) {
                sawEnabled |= r.isEnabled();
                builtConfig.add(JsonBuilder.buildIfBuilder(r));
            }
            if (!sawEnabled) {
                builtConfig.add(RegistryConfigImpl.getDefaultRegistry());
            }
            this.registries = Collections.unmodifiableList(builtConfig);
        }
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @Override
    @JsonIgnore
    public ConfigSource getSource() {
        return configSource;
    }

    @JsonIgnore
    RegistriesConfigImpl setSource(ConfigSource configSource) {
        // package private.
        // Source isn't known during deserialization. Must be added post-construction.
        this.configSource = configSource;
        return this;
    }

    @Override
    public void persist() throws IOException {
        persistConfigSource(this);
    }

    /**
     * Builder.
     * {@literal set*} methods are used for deserialization
     */
    @JsonDeserialize(using = RegistriesConfigImpl.Deserializer.class)
    public static class Builder implements RegistriesConfig.Mutable {
        protected boolean debug;
        protected final List<RegistryConfig> registries;
        protected ConfigSource configSource = ConfigSource.MANUAL;

        public Builder() {
            registries = new ArrayList<>();
        }

        @JsonIgnore
        Builder(RegistriesConfig config) {
            this.debug = config.isDebug();
            this.registries = new ArrayList<>(config.getRegistries());
            this.configSource = config.getSource();
        }

        @JsonIgnore
        public Builder setRegistry(String registryId) {
            addRegistry(registryId);
            return this;
        }

        @JsonIgnore
        public Builder setRegistry(RegistryConfig config) {
            addRegistry(config);
            return this;
        }

        @Override
        public boolean addRegistry(String registryId) {
            return addRegistry(RegistryConfig.builder()
                    .setId(registryId)
                    .build());
        }

        @Override
        public boolean addRegistry(RegistryConfig config) {
            if (registries.stream().anyMatch(r -> r.getId().equals(config.getId()))) {
                return false;
            }
            return registries.add(config);
        }

        @Override
        public boolean removeRegistry(String registryId) {
            return registries.removeIf(r -> r.getId().equals(registryId));
        }

        @Override
        public boolean isDebug() {
            return debug;
        }

        public Builder setDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        @Override
        public List<RegistryConfig> getRegistries() {
            return registries;
        }

        public Builder setRegistries(List<RegistryConfig> registries) {
            this.registries.clear();
            this.registries.addAll(registries);
            return this;
        }

        @Override
        public RegistriesConfigImpl build() {
            return new RegistriesConfigImpl(this);
        }

        @Override
        public void persist() throws IOException {
            persistConfigSource(this.build());
        }

        @Override
        @JsonIgnore
        public ConfigSource getSource() {
            return configSource;
        }

        @JsonIgnore
        public Builder setSource(ConfigSource configSource) {
            // Source isn't known during deserialization. Must be added post-construction.
            this.configSource = configSource;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegistriesConfigImpl that = (RegistriesConfigImpl) o;
        return debug == that.debug && Objects.equals(registries, that.registries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(debug, registries);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() +
                "{debug=" + debug +
                ", registries=" + registries +
                ", configSource=" + configSource +
                '}';
    }

    static void persistConfigSource(RegistriesConfigImpl config) throws IOException {
        Path targetFile = config.configSource.getFilePath();
        if (config.configSource == ConfigSource.DEFAULT) {
            targetFile = RegistriesConfigLocator.getDefaultConfigYamlLocation();
        } else if (targetFile == null) {
            throw new UnsupportedOperationException(
                    String.format("Can not write configuration as it was read from an alternate source: %s",
                            config.configSource.describe()));
        }
        config.persist(targetFile);
    }

    static class Serializer extends JsonSerializer<RegistriesConfigImpl> {
        @Override
        public void serialize(RegistriesConfigImpl value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            boolean isDefaultDebug = !value.debug;
            boolean isDefaultList = RegistryConfigImpl.isDefaultList(value.registries);

            if (isDefaultDebug && isDefaultList) {
                gen.writeNumber("");
            } else {
                gen.writeStartObject();
                if (gen instanceof YAMLGenerator) {
                    if (!isDefaultDebug) {
                        gen.writeObjectField("debug", true);
                    }
                    if (!isDefaultList) {
                        gen.writeObjectField("registries", value.registries);
                    }
                } else {
                    for (RegistryConfig x : value.registries) {
                        gen.writeObjectFieldStart(x.getId());
                        RegistryConfigImpl.Serializer.writeContents(x, gen);
                        gen.writeEndObject();
                    }
                }
                gen.writeEndObject();
            }
        }
    }

    static class Deserializer extends JsonDeserializer<RegistriesConfigImpl.Builder> {
        final static RegistryConfigImpl.BuilderDeserializer DESERIALIZER = new RegistryConfigImpl.BuilderDeserializer();

        @Override
        public RegistriesConfigImpl.Builder deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonStreamContext ctx = p.getParsingContext();
            final RegistriesConfigImpl.Builder builder = new Builder();

            while (p.nextValue() != null) {
                if ("debug".equals(p.getCurrentName())) {
                    builder.setDebug(p.getBooleanValue());
                } else if ("registries".equals(p.getCurrentName())) {
                    while (p.nextValue() != JsonToken.END_ARRAY) {
                        RegistryConfigImpl.Builder config = DESERIALIZER.deserialize(p, ctxt);
                        if (config != null) {
                            builder.addRegistry(config);
                        }
                    }
                    JsonBuilder.ensureNextToken(p, JsonToken.END_OBJECT, ctxt);
                }
            }

            return builder;
        }
    }
}
