package io.quarkus.liquibase.runtime;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.runtime.DatabaseSchemaProvider;
import io.quarkus.liquibase.LiquibaseFactory;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

public class LiquibaseSchemaProvider implements DatabaseSchemaProvider {
    @Override
    public void resetDatabase(String dbName) {
        try {
            try {
                LiquibaseFactory liquibaseFactory = LiquibaseFactoryUtil.getLiquibaseFactory(dbName).get();
                doReset(liquibaseFactory);
            } catch (UnsatisfiedResolutionException e) {
                //ignore, the DS is not configured
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error starting Liquibase", e);
        }
    }

    @Override
    public void resetAllDatabases() {
        try {
            for (InstanceHandle<LiquibaseFactory> liquibaseFactoryHandle : LiquibaseFactoryUtil.getActiveLiquibaseFactories()) {
                try {
                    LiquibaseFactory liquibaseFactory = liquibaseFactoryHandle.get();
                    doReset(liquibaseFactory);
                } catch (UnsatisfiedResolutionException e) {
                    //ignore, the DS is not configured
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error starting Liquibase", e);
        }
    }

    public void doReset(LiquibaseFactory liquibaseFactory) throws LiquibaseException {
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.dropAll();
        }
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
        }
    }
}
