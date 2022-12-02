package io.quarkus.registry.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see RegistryMavenConfig#builder() creates a builder
 * @see RegistryMavenConfig#mutable() creates a builder from an existing RegistryMavenConfig
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryMavenConfigImpl implements RegistryMavenConfig {

    private final RegistryMavenRepoConfig repository;

    private RegistryMavenConfigImpl(RegistryMavenRepoConfig repository) {
        this.repository = JsonBuilder.buildIfBuilder(repository);
    }

    @Override
    public RegistryMavenRepoConfig getRepository() {
        return repository;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegistryMavenConfigImpl that = (RegistryMavenConfigImpl) o;
        return Objects.equals(repository, that.repository);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() +
                "{repository=" + repository +
                '}';
    }

    /**
     * Builder.
     */
    public static class Builder implements RegistryMavenConfig.Mutable {

        protected RegistryMavenRepoConfig repository;

        public Builder() {
        }

        @JsonIgnore
        Builder(RegistryMavenConfig config) {
            this.repository = config.getRepository();
        }

        @Override
        public RegistryMavenRepoConfig getRepository() {
            return repository;
        }

        @JsonDeserialize(as = RegistryMavenRepoConfigImpl.Builder.class)
        public Builder setRepository(RegistryMavenRepoConfig repository) {
            this.repository = repository;
            return this;
        }

        @Override
        public RegistryMavenConfigImpl build() {
            return new RegistryMavenConfigImpl(repository);
        }
    }
}
