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
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.cfg.Environment", "HHH000206"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.bytecode.enhance.spi.Enhancer", "Enhancing [%s] as"));
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.bytecode.enhance.internal.bytebuddy.BiDirectionalAssociationHandler", "Could not find"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.jpa.internal.util.LogHelper", "HHH000204"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.annotations.common.Version", "HCANN000001"));
        filters.produce(
                new LogCleanupFilterBuildItem("org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl", "HHH000422"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.dialect.Dialect", "HHH000400"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.type.BasicTypeRegistry", "HHH000270"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.orm.beans", "HHH10005002", "HHH10005004"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tuple.PojoInstantiator", "HHH000182"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tuple.entity.EntityMetamodel", "HHH000157"));
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator", "HHH000490"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tool.schema.internal.SchemaCreatorImpl", "HHH000476"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.jpa.boot.internal.PersistenceXmlParser", "HHH000318"));
    }
}
