package io.quarkus.liquibase.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.sql.DataSource;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;

/**
 * The liquibase recorder
 */
@Recorder
public class LiquibaseRecorder {

    private final List<LiquibaseContainer> liquibaseContainers = new ArrayList<>(2);

    public Supplier<LiquibaseFactory> liquibaseSupplier(String dataSourceName) {
        DataSource dataSource = DataSources.fromName(dataSourceName);
        LiquibaseContainerProducer liquibaseProducer = Arc.container().instance(LiquibaseContainerProducer.class).get();
        LiquibaseContainer liquibaseContainer = liquibaseProducer.createLiquibaseFactory(dataSource, dataSourceName);
        liquibaseContainers.add(liquibaseContainer);
        return new Supplier<LiquibaseFactory>() {
            @Override
            public LiquibaseFactory get() {
                return liquibaseContainer.getLiquibaseFactory();
            }
        };
    }

    public void doStartActions() {
        try {
            for (LiquibaseContainer liquibaseContainer : liquibaseContainers) {
                LiquibaseFactory liquibaseFactory = liquibaseContainer.getLiquibaseFactory();
                if (liquibaseContainer.isCleanAtStart()) {
                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        liquibase.dropAll();
                    }
                }
                if (liquibaseContainer.isMigrateAtStart()) {
                    if (liquibaseContainer.isValidateOnMigrate()) {
                        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                            liquibase.validate();
                        }
                    }
                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
