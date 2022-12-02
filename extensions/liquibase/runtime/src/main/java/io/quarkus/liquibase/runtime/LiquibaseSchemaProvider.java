package io.quarkus.liquibase.runtime;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.runtime.DatabaseSchemaProvider;
import io.quarkus.liquibase.LiquibaseFactory;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

public class LiquibaseSchemaProvider implements DatabaseSchemaProvider {
    @Override
    public void resetDatabase(String dbName) {
        try {
            InjectableInstance<LiquibaseFactory> liquibaseFactoryInstance = Arc.container()
                    .select(LiquibaseFactory.class, Any.Literal.INSTANCE);
            if (liquibaseFactoryInstance.isUnsatisfied()) {
                return;
            }
            for (InstanceHandle<LiquibaseFactory> liquibaseFactoryHandle : liquibaseFactoryInstance.handles()) {
                try {
                    LiquibaseFactory liquibaseFactory = liquibaseFactoryHandle.get();
                    if (liquibaseFactory.getDataSourceName().equals(dbName)) {
                        doReset(liquibaseFactory);
                    }
                } catch (UnsatisfiedResolutionException e) {
                    //ignore, the DS is not configured
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error starting Liquibase", e);
        }
    }

    @Override
    public void resetAllDatabases() {
        try {
            InjectableInstance<LiquibaseFactory> liquibaseFactoryInstance = Arc.container()
                    .select(LiquibaseFactory.class, Any.Literal.INSTANCE);
            if (liquibaseFactoryInstance.isUnsatisfied()) {
                return;
            }
            for (InstanceHandle<LiquibaseFactory> liquibaseFactoryHandle : liquibaseFactoryInstance.handles()) {
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
