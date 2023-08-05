package io.quarkus.liquibase.runtime;

import java.util.function.Function;

import javax.sql.DataSource;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LiquibaseRecorder {

    private final RuntimeValue<LiquibaseRuntimeConfig> config;

    public LiquibaseRecorder(RuntimeValue<LiquibaseRuntimeConfig> config) {
        this.config = config;
    }

    public Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory> liquibaseFunction(String dataSourceName) {
        DataSource dataSource = DataSources.fromName(dataSourceName);
        if (dataSource instanceof UnconfiguredDataSource) {
            return new Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory>() {
                @Override
                public LiquibaseFactory apply(SyntheticCreationalContext<LiquibaseFactory> context) {
                    throw new UnsatisfiedResolutionException("No datasource has been configured");
                }
            };
        }
        return new Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory>() {
            @Override
            public LiquibaseFactory apply(SyntheticCreationalContext<LiquibaseFactory> context) {
                LiquibaseFactoryProducer liquibaseProducer = context.getInjectedReference(LiquibaseFactoryProducer.class);
                LiquibaseFactory liquibaseFactory = liquibaseProducer.createLiquibaseFactory(dataSource, dataSourceName);
                return liquibaseFactory;
            }
        };
    }
}
