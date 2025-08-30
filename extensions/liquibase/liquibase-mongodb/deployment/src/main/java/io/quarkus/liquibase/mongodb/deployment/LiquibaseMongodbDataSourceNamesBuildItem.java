package io.quarkus.liquibase.mongodb.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class LiquibaseMongodbDataSourceNamesBuildItem extends SimpleBuildItem {
    private final Set<String> dataSourceNames;

    public LiquibaseMongodbDataSourceNamesBuildItem(Set<String> dataSourceNames) {
        this.dataSourceNames = dataSourceNames;
    }

    public Set<String> getDataSourceNames() {
        return dataSourceNames;
    }
}
