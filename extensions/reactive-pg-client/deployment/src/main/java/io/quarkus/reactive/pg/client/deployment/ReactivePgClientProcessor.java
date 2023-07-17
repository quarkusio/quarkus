package io.quarkus.reactive.pg.client.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.devconsole.Name;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.pg.client.PgPoolCreator;
import io.quarkus.reactive.pg.client.runtime.DataSourcesReactivePostgreSQLConfig;
import io.quarkus.reactive.pg.client.runtime.PgPoolRecorder;
import io.quarkus.reactive.pg.client.runtime.PostgreSQLServiceBindingConverter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;

class ReactivePgClientProcessor {

    @BuildStep
    NativeImageConfigBuildItem config() {
        return NativeImageConfigBuildItem.builder().addRuntimeInitializedClass("io.vertx.pgclient.impl.codec.StartupMessage")
                .build();
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.reactive(DatabaseKind.POSTGRESQL);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<PgPoolBuildItem> pgPool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            PgPoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactivePostgreSQLConfig dataSourcesReactivePostgreSQLConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_PG_CLIENT));

        createPoolIfDefined(recorder, vertx, eventLoopCount, shutdown, pgPool, vertxPool, syntheticBeans,
                DataSourceUtil.DEFAULT_DATASOURCE_NAME, dataSourcesBuildTimeConfig,
                dataSourcesRuntimeConfig, dataSourcesReactiveBuildTimeConfig, dataSourcesReactiveRuntimeConfig,
                dataSourcesReactivePostgreSQLConfig, defaultDataSourceDbKindBuildItems, curateOutcomeBuildItem);

        for (String dataSourceName : dataSourcesBuildTimeConfig.namedDataSources.keySet()) {
            createPoolIfDefined(recorder, vertx, eventLoopCount, shutdown, pgPool, vertxPool, syntheticBeans, dataSourceName,
                    dataSourcesBuildTimeConfig, dataSourcesRuntimeConfig, dataSourcesReactiveBuildTimeConfig,
                    dataSourcesReactiveRuntimeConfig, dataSourcesReactivePostgreSQLConfig, defaultDataSourceDbKindBuildItems,
                    curateOutcomeBuildItem);
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_PG_CLIENT));

        return new ServiceStartBuildItem("reactive-pg-client");
    }

    /**
     * The health check needs to be produced in a separate method to avoid a circular dependency (the Vert.x instance creation
     * consumes the AdditionalBeanBuildItems).
     */
    @BuildStep
    void addHealthCheck(
            Capabilities capabilities,
            BuildProducer<HealthBuildItem> healthChecks,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (!capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return;
        }

        if (!hasPools(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig, defaultDataSourceDbKindBuildItems,
                curateOutcomeBuildItem)) {
            return;
        }

        healthChecks
                .produce(new HealthBuildItem("io.quarkus.reactive.pg.client.runtime.health.ReactivePgDataSourcesHealthCheck",
                        dataSourcesBuildTimeConfig.healthEnabled));
    }

    @BuildStep
    void unremoveableBeans(BuildProducer<UnremovableBeanBuildItem> producer) {
        producer.produce(UnremovableBeanBuildItem.beanTypes(PgPoolCreator.class));
    }

    @BuildStep
    void validateBeans(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        // no two PgPoolCreator beans can be associated with the same datasource
        Map<String, Boolean> seen = new HashMap<>();
        for (BeanInfo beanInfo : validationPhase.getContext().beans().matchBeanTypes(new PgPoolCreatorBeanClassPredicate())) {
            Set<Name> qualifiers = new TreeSet<>(); // use a TreeSet in order to get a predictable iteration order
            for (AnnotationInstance qualifier : beanInfo.getQualifiers()) {
                qualifiers.add(Name.from(qualifier));
            }
            String qualifiersStr = qualifiers.stream().map(Name::toString).collect(Collectors.joining("_"));
            if (seen.getOrDefault(qualifiersStr, false)) {
                errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                        new IllegalStateException(
                                "There can be at most one bean of type '" + PgPoolCreator.class.getName()
                                        + "' for each datasource.")));
            } else {
                seen.put(qualifiersStr, true);
            }
        }
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            PostgreSQLServiceBindingConverter.class.getName()));
        }
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.POSTGRESQL));
    }

    private void createPoolIfDefined(PgPoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            BuildProducer<PgPoolBuildItem> pgPool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            String dataSourceName,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactivePostgreSQLConfig dataSourcesReactivePostgreSQLConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        if (!isReactivePostgreSQLPoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig, dataSourceName,
                defaultDataSourceDbKindBuildItems, curateOutcomeBuildItem)) {
            return;
        }

        RuntimeValue<PgPool> pool = recorder.configurePgPool(vertx.getVertx(),
                eventLoopCount.getEventLoopCount(),
                dataSourceName,
                dataSourcesRuntimeConfig,
                dataSourcesReactiveRuntimeConfig,
                dataSourcesReactivePostgreSQLConfig,
                shutdown);
        pgPool.produce(new PgPoolBuildItem(dataSourceName, pool));

        ExtendedBeanConfigurator pgPoolBeanConfigurator = SyntheticBeanBuildItem.configure(PgPool.class)
                .defaultBean()
                .addType(Pool.class)
                .scope(ApplicationScoped.class)
                .runtimeValue(pool)
                .unremovable()
                .setRuntimeInit();

        addQualifiers(pgPoolBeanConfigurator, dataSourceName);

        syntheticBeans.produce(pgPoolBeanConfigurator.done());

        ExtendedBeanConfigurator mutinyPgPoolConfigurator = SyntheticBeanBuildItem
                .configure(io.vertx.mutiny.pgclient.PgPool.class)
                .defaultBean()
                .scope(ApplicationScoped.class)
                .runtimeValue(recorder.mutinyPgPool(pool))
                .setRuntimeInit();

        addQualifiers(mutinyPgPoolConfigurator, dataSourceName);

        syntheticBeans.produce(mutinyPgPoolConfigurator.done());

        vertxPool.produce(new VertxPoolBuildItem(pool, DatabaseKind.POSTGRESQL, DataSourceUtil.isDefault(dataSourceName)));
    }

    private static boolean isReactivePostgreSQLPoolDefined(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig, String dataSourceName,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        DataSourceBuildTimeConfig dataSourceBuildTimeConfig = dataSourcesBuildTimeConfig
                .getDataSourceRuntimeConfig(dataSourceName);
        DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig = dataSourcesReactiveBuildTimeConfig
                .getDataSourceReactiveBuildTimeConfig(dataSourceName);

        Optional<String> dbKind = DefaultDataSourceDbKindBuildItem.resolve(dataSourceBuildTimeConfig.dbKind,
                defaultDataSourceDbKindBuildItems,
                !DataSourceUtil.isDefault(dataSourceName) || dataSourceBuildTimeConfig.devservices.enabled
                        .orElse(dataSourcesBuildTimeConfig.namedDataSources.isEmpty()),
                curateOutcomeBuildItem);

        if (!dbKind.isPresent()) {
            return false;
        }

        if (!DatabaseKind.isPostgreSQL(dbKind.get())
                || !dataSourceReactiveBuildTimeConfig.enabled) {
            return false;
        }

        return true;
    }

    private boolean hasPools(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (isReactivePostgreSQLPoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig,
                DataSourceUtil.DEFAULT_DATASOURCE_NAME, defaultDataSourceDbKindBuildItems, curateOutcomeBuildItem)) {
            return true;
        }

        for (String dataSourceName : dataSourcesBuildTimeConfig.namedDataSources.keySet()) {
            if (isReactivePostgreSQLPoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig,
                    dataSourceName, defaultDataSourceDbKindBuildItems, curateOutcomeBuildItem)) {
                return true;
            }
        }

        return false;
    }

    private static void addQualifiers(ExtendedBeanConfigurator configurator, String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            configurator.addQualifier(DotNames.DEFAULT);
        } else {
            configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", dataSourceName).done();
            configurator.addQualifier().annotation(ReactiveDataSource.class).addValue("value", dataSourceName)
                    .done();
        }
    }

    private static class PgPoolCreatorBeanClassPredicate implements Predicate<Set<Type>> {
        private static final Type PG_POOL_CREATOR = Type.create(DotName.createSimple(PgPoolCreator.class.getName()),
                Type.Kind.CLASS);

        @Override
        public boolean test(Set<Type> types) {
            return types.contains(PG_POOL_CREATOR);
        }
    }
}
