package io.quarkus.hibernate.reactive.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;

@BuildSteps(onlyIf = HibernateReactiveEnabled.class)
public final class HibernateReactiveLogFilter {

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator", "HHH000181"));
        //See https://hibernate.atlassian.net/browse/HHH-16224
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.dialect.PostgreSQLPGObjectJdbcType", "HHH000514"));
    }

}
