package io.quarkus.it.hibernate.search.standalone.elasticsearch.layout;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;

@ApplicationScoped
@Named("CustomIndexLayoutStrategy")
public class CustomIndexLayoutStrategy implements IndexLayoutStrategy {

    @ConfigProperty(name = "test.index-layout.prefix", defaultValue = "-")
    String prefix;

    @Override
    public String createInitialElasticsearchIndexName(String hibernateSearchIndexName) {
        return "%s%s-000001".formatted(prefix, hibernateSearchIndexName);
    }

    @Override
    public String createWriteAlias(String hibernateSearchIndexName) {
        return "%s%s-write".formatted(prefix, hibernateSearchIndexName);
    }

    @Override
    public String createReadAlias(String hibernateSearchIndexName) {
        return "%s%s-read".formatted(prefix, hibernateSearchIndexName);
    }
}
