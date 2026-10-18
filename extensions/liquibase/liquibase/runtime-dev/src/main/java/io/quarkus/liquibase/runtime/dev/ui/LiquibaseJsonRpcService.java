package io.quarkus.liquibase.runtime.dev.ui;

import java.util.Collection;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.liquibase.LiquibaseFactory;
import liquibase.Liquibase;

@ApplicationScoped
public class LiquibaseJsonRpcService {

    private Collection<LiquibaseFactory> factories;

    @PostConstruct
    void init() {
        factories = new LiquibaseFactoriesSupplier().get();
    }

    public boolean clear(String ds) throws Exception {
        for (LiquibaseFactory lf : factories) {
            if (ds.equalsIgnoreCase(lf.getDataSourceName())) {
                try (Liquibase liquibase = lf.createLiquibase()) {
                    liquibase.dropAll();
                }
                return true;
            }
        }
        return false;
    }

    public boolean migrate(String ds) throws Exception {
        for (LiquibaseFactory lf : factories) {
            if (ds.equalsIgnoreCase(lf.getDataSourceName())) {
                try (Liquibase liquibase = lf.createLiquibase()) {
                    liquibase.update(lf.createContexts(), lf.createLabels());
                }
                return true;
            }
        }
        return false;
    }

    public Integer getDatasourceCount() {
        return factories.size();
    }

    public Collection<LiquibaseFactory> getLiquibaseFactories() {
        return factories;
    }
}
