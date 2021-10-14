package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;

@JsonDeserialize(builder = RegistryMavenConfigImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryMavenConfigImpl implements RegistryMavenConfig {

    private final RegistryMavenRepoConfig repository;

    private RegistryMavenConfigImpl(Builder builder) {
        this.repository = builder.repository;
    }

    @Override
    public RegistryMavenRepoConfig getRepository() {
        return repository;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder implements RegistryMavenConfig {

        protected RegistryMavenRepoConfig repository;

        public Builder() {
        }

        @JsonIgnore
        public Builder(RegistryMavenConfig config) {
            this.repository = config.getRepository();
        }

        @JsonDeserialize(as = RegistryMavenRepoConfigImpl.class)
        public Builder withRepository(RegistryMavenRepoConfig repository) {
            this.repository = repository;
            return this;
        }

        @Override
        public RegistryMavenRepoConfig getRepository() {
            return null;
        }

        public RegistryMavenConfigImpl build() {
            return new RegistryMavenConfigImpl(this);
        }
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
        return "BaseRegistryMavenConfig{" +
                "repository=" + repository +
                '}';
    }
}
