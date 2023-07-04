package io.quarkus.liquibase.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.runtime.annotations.PreStart;
import liquibase.Liquibase;
import liquibase.lockservice.LockServiceFactory;

@ApplicationScoped
@PreStart("liquibase-init-task")
public class LiquibaseInitTask implements Runnable {

    @Inject
    @Any
    Instance<LiquibaseFactory> liquibaseFactories;

    @Inject
    @ConfigProperty(name = "quarkus.liquibase.enabled")
    boolean enabled;

    @Override
    public void run() {
        if (!enabled) {
            return;
        }

        try {
            for (LiquibaseFactory liquibaseFactory : liquibaseFactories) {
                try {
                    LiquibaseConfig config = liquibaseFactory.getConfiguration();
                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        if (config.cleanAtStart) {
                            liquibase.dropAll();
                        }
                        if (config.migrateAtStart) {
                            var lockService = LockServiceFactory.getInstance()
                                    .getLockService(liquibase.getDatabase());
                            lockService.waitForLock();
                            try {
                                if (config.validateOnMigrate) {
                                    liquibase.validate();
                                }
                                liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
                            } finally {
                                lockService.releaseLock();
                            }
                        }
                    }
                } catch (UnsatisfiedResolutionException e) {
                    //ignore, the DS is not configured
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error starting Liquibase", e);
        }
    }
}
