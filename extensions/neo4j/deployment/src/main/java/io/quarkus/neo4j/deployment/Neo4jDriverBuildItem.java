package io.quarkus.neo4j.deployment;

import org.neo4j.driver.Driver;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Allows access to the Neo4j Driver instance from within other extensions.
 */
public final class Neo4jDriverBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Driver> value;

    public Neo4jDriverBuildItem(RuntimeValue<Driver> value) {
        this.value = value;
    }

    public RuntimeValue<Driver> getValue() {
        return value;
    }
}
