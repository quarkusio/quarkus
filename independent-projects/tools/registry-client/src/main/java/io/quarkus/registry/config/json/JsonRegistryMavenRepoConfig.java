package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.registry.config.RegistryMavenRepoConfig;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRegistryMavenRepoConfig implements RegistryMavenRepoConfig.Mutable {

    private String id;
    private String url;

    @Override
    public String getId() {
        return id;
    }

    public Mutable setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public Mutable setUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    public String toString() {
        return "[id=" + id + ", url=" + url + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonRegistryMavenRepoConfig other = (JsonRegistryMavenRepoConfig) obj;
        return Objects.equals(id, other.id) && Objects.equals(url, other.url);
    }

    public Mutable mutable() {
        return new JsonRegistryMavenRepoConfig()
                .setId(this.id)
                .setUrl(this.url);
    }

    public JsonRegistryMavenRepoConfig build() {
        return this;
    }
}
