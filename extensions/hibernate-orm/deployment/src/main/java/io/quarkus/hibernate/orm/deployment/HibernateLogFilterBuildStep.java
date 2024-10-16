package io.quarkus.hibernate.orm.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.hibernate.orm.runtime.graal.DisableLoggingFeature;

/**
 * Processor that sets up log filters for Hibernate
 */
@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class HibernateLogFilterBuildStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(DisableLoggingFeature.class);
    }

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.Version", "HHH000412"));
        //Disable details about bytecode reflection optimizer:
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.cfg.Environment", "HHH000406"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.jpa.internal.util.LogHelper", "HHH000204"));
        filters.produce(new LogCleanupFilterBuildItem("SQL dialect", "HHH000400"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.orm.beans", "HHH10005002", "HHH10005004"));
        // Silence incubating settings warnings as we will use some for compatibility
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.orm.incubating",
                "HHH90006001"));

        // Silence DB connection info logging because:
        // 1. We don't implement the retrieval of information in QuarkusConnectionProvider
        // 2. It's currently being logged even at static init when there is no connection
        //    See https://hibernate.atlassian.net/browse/HHH-18454
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.orm.connections.pooling",
                "HHH10001005"));

        //This "deprecation" warning isn't practical for the specific Quarkus needs, as it reminds users they don't need
        //to set the 'hibernate.dialect' property, however it's being set by Quarkus buildsteps so they can't avoid it.
        //Ignore for now, perhaps remove it upstream however this may make sense for other Hibernate users.
        //Wondering if we should have the Quarkus build differentiate between an explicitly set vs an inferred Dialect
        //property (we have a custom DialectFactory already so this could be trivial), however even in this case ORM
        //can't guess things since there is no connection, so even if we did so, this message wouldn't be applicable.
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.orm.deprecation", "HHH90000025"));
    }
}
