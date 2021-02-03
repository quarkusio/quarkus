package io.quarkus.registry.builder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.quarkus.registry.model.ImmutableRegistry;
import io.quarkus.registry.model.Registry;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class URLRegistryBuilder {

    private final List<URL> urls = new ArrayList<>();

    public URLRegistryBuilder addURL(URL url) {
        urls.add(url);
        return this;
    }

    public URLRegistryBuilder addURLs(Collection<URL> urls) {
        this.urls.addAll(urls);
        return this;
    }

    public Registry build() throws IOException {
        if (urls.isEmpty()) {
            throw new IllegalStateException("At least one URL must be specified");
        }
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        if (urls.size() == 1) {
            // Just one
            return mapper.readValue(urls.get(0), Registry.class);
        } else {
            ImmutableRegistry.Builder builder = Registry.builder();
            for (URL url : urls) {
                Registry aRegistry = mapper.readValue(url, Registry.class);
                builder.addAllCategories(aRegistry.getCategories())
                        .addAllExtensions(aRegistry.getExtensions())
                        .addAllPlatforms(aRegistry.getPlatforms())
                        .putAllCoreVersions(aRegistry.getCoreVersions());
            }
            return builder.build();
        }

    }
}
