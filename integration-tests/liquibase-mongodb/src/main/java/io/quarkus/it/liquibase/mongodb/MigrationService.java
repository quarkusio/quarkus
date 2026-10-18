package io.quarkus.it.liquibase.mongodb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.liquibase.mongodb.LiquibaseMongodbFactory;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbClient;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

@ApplicationScoped
public class MigrationService {
    // You can Inject the object if you want to use it manually
    @Inject
    @LiquibaseMongodbClient("inventory")
    LiquibaseMongodbFactory liquibaseFactory;

    public void update() throws LiquibaseException {
        // Use the liquibase instance manually
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
        }
    }
}
