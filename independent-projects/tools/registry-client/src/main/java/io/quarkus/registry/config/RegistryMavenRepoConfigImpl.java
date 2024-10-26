package io.quarkus.registry.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see RegistryMavenRepoConfig#builder() creates a builder
 * @see RegistryMavenRepoConfig#mutable() creates a builder from an existing RegistriesConfig
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "id", "url" })
public class RegistryMavenRepoConfigImpl implements RegistryMavenRepoConfig {

    private final String id;
    private final String url;

    private RegistryMavenRepoConfigImpl(String id, String url) {
        this.id = id;
        this.url = url;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegistryMavenRepoConfigImpl that = (RegistryMavenRepoConfigImpl) o;
        return Objects.equals(id, that.id) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url);
    }

    @Override
    public String toString() {
        return "BaseRegistryMavenRepoConfig{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    /**
     * Builder.
     */
    public static class Builder implements RegistryMavenRepoConfig.Mutable {

        protected String id;
        protected String url;

        public Builder() {
        }

        @JsonIgnore
        Builder(RegistryMavenRepoConfig config) {
            this.id = config.getId();
            this.url = config.getUrl();
        }

        @Override
        public String getId() {
            return this.id;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        @Override
        public String getUrl() {
            return this.url;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        @Override
        public RegistryMavenRepoConfigImpl build() {
            return new RegistryMavenRepoConfigImpl(id, url);
        }
    }
}
