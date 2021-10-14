package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;

@JsonDeserialize(builder = RegistryMavenRepoConfigImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryMavenRepoConfigImpl implements RegistryMavenRepoConfig {

    private final String id;
    private final String url;

    private RegistryMavenRepoConfigImpl(Builder builder) {
        this.id = builder.id;
        this.url = builder.url;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder implements RegistryMavenRepoConfig {

        protected String id;
        protected String url;

        public Builder() {
        }

        @JsonIgnore
        public Builder(RegistryMavenRepoConfigImpl config) {
            this.id = config.getId();
            this.url = config.getUrl();
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public String getUrl() {
            return this.url;
        }

        public RegistryMavenRepoConfigImpl build() {
            return new RegistryMavenRepoConfigImpl(this);
        }
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
}
