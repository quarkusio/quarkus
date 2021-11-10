package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Top of the config hierarchy. Holder of the rest of the things.
 * Read this using {@literal ObjectMapper.readValue(..., BaseRegistriesConfig.class)}.
 * Though note that, per the {@link Builder#build()} method, a {@link RegistriesConfig}
 * object should be returned.
 *
 * @see Builder#build ensures the list of RegistryConfig definitions is not empty
 * @see io.quarkus.registry.config.RegistriesConfigLocator#load(Path)
 */
@JsonDeserialize(builder = RegistriesConfigImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistriesConfigImpl implements RegistriesConfig {
    private final boolean debug;
    private final List<RegistryConfig> registries;
    private ConfigSource configSource = ConfigSource.MANUAL;

    private RegistriesConfigImpl(boolean debug, List<RegistryConfig> registries) {
        this.debug = debug;
        this.registries = Collections.unmodifiableList(registries);
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
    public MutableRegistriesConfig mutable() {
        return new Builder(this);
    }

    @Override
    @JsonIgnore
    public ConfigSource getSource() {
        return configSource;
    }

    @JsonIgnore
    RegistriesConfigImpl setConfigSource(ConfigSource configSource) {
        // package private.
        // Source isn't known during deserialization. Must be added post-construction.
        this.configSource = configSource;
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder implements MutableRegistriesConfig {
        protected boolean debug;
        protected List<RegistryConfig> registries;
        protected ConfigSource configSource = ConfigSource.MANUAL;

        public Builder() {
            registries = new ArrayList<>();
        }

        @JsonIgnore
        public Builder(RegistriesConfig immutableSource) {
            this.debug = immutableSource.isDebug();
            this.registries = new ArrayList<>(immutableSource.getRegistries());
        }

        public Builder withDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        @JsonDeserialize(using = RegistryConfigImpl.Deserializer.class)
        public Builder withRegistries(List<RegistryConfig> registries) {
            this.registries = registries;
            return this;
        }

        @JsonIgnore
        public Builder withRegistry(String registryId) {
            addRegistry(registryId);
            return this;
        }

        @JsonIgnore
        public Builder withRegistry(RegistryConfig config) {
            addRegistry(config);
            return this;
        }

        @Override
        public boolean addRegistry(String registryId) {
            return addRegistry(RegistryConfigImpl.builder()
                    .withId(registryId)
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

        @JsonIgnore
        public void setConfigSource() {
            // Source isn't known during deserialization. Must be added post-construction.
            this.configSource = configSource;
        }

        /**
         * Complete any/all required attributes. Namely, ensure the list
         * of registries contains at least one enabled registry (the default).
         */
        public RegistriesConfigImpl build() {
            final List<RegistryConfig> builtConfig;

            if (registries.isEmpty()) {
                builtConfig = Collections.singletonList(RegistryConfigImpl.getDefaultRegistry());
            } else {
                builtConfig = new ArrayList<>(registries.size());
                boolean sawEnabled = false;
                for (RegistryConfig r : registries) {
                    sawEnabled |= r.isEnabled();
                    builtConfig.add(r instanceof RegistryConfigImpl.Builder
                            ? ((RegistryConfigImpl.Builder) r).build()
                            : r);
                }
                if (!sawEnabled) {
                    builtConfig.add(RegistryConfigImpl.getDefaultRegistry());
                }
            }

            return new RegistriesConfigImpl(this.debug, builtConfig);
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
        public Builder mutable() {
            return this;
        }

        @Override
        @JsonIgnore
        public ConfigSource getSource() {
            return configSource;
        }

        @Override
        public void persist() throws IOException {
            Path targetFile = configSource.getFilePath();
            if (targetFile == null) {
                throw new UnsupportedOperationException(
                        String.format("Can not write configuration as it was read from an alternate source: %s",
                                configSource.describe()));
            }
            persist(targetFile);
        }

        @Override
        public void persist(Path targetFile) throws IOException {
            RegistriesConfigMapperHelper.serialize(build(), targetFile);
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
        return "BaseRegistriesConfig{" +
                "debug=" + debug +
                ", registries=" + registries +
                ", configSource=" + configSource +
                '}';
    }
}
