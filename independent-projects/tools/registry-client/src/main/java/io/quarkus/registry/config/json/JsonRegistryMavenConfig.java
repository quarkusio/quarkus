package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.config.RegistryMavenConfig;
import io.quarkus.registry.config.RegistryMavenRepoConfig;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRegistryMavenConfig implements RegistryMavenConfig {

    private RegistryMavenRepoConfig repo;

    @Override
    @JsonDeserialize(as = JsonRegistryMavenRepoConfig.class)
    public RegistryMavenRepoConfig getRepository() {
        return repo;
    }

    public void setRepository(RegistryMavenRepoConfig repo) {
        this.repo = repo;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append("repo=").append(repo);
        return buf.append(']').toString();
    }
}
