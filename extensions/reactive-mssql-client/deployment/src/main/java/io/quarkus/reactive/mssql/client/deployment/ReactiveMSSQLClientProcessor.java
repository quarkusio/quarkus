package io.quarkus.reactive.mssql.client.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.devui.Name;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourceSupport;
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
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.mssql.client.MSSQLPoolCreator;
import io.quarkus.reactive.mssql.client.runtime.DataSourcesReactiveMSSQLConfig;
import io.quarkus.reactive.mssql.client.runtime.MSSQLPoolRecorder;
import io.quarkus.reactive.mssql.client.runtime.MsSQLServiceBindingConverter;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.Pool;

class ReactiveMSSQLClientProcessor {

    private static final ParameterizedType POOL_INJECTION_TYPE = ParameterizedType.create(DotName.createSimple(Instance.class),
            new Type[] { ClassType.create(DotName.createSimple(MSSQLPoolCreator.class.getName())) }, null);
    private static final AnnotationInstance[] EMPTY_ANNOTATIONS = new AnnotationInstance[0];
    private static final DotName REACTIVE_DATASOURCE = DotName.createSimple(ReactiveDataSource.class);

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<MSSQLPoolBuildItem> msSQLPool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            MSSQLPoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactiveMSSQLConfig dataSourcesReactiveMSSQLConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_MSSQL_CLIENT));

        for (String dataSourceName : dataSourcesBuildTimeConfig.dataSources().keySet()) {
            createPoolIfDefined(recorder, vertx, eventLoopCount, shutdown, msSQLPool, syntheticBeans, dataSourceName,
                    dataSourcesBuildTimeConfig, dataSourcesRuntimeConfig, dataSourcesReactiveBuildTimeConfig,
                    dataSourcesReactiveRuntimeConfig, dataSourcesReactiveMSSQLConfig, defaultDataSourceDbKindBuildItems,
                    curateOutcomeBuildItem);
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_MSSQL_CLIENT));

        vertxPool.produce(new VertxPoolBuildItem());
        return new ServiceStartBuildItem("reactive-mssql-client");
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.reactive(DatabaseKind.MSSQL);
    }

    @BuildStep
    void unremoveableBeans(BuildProducer<UnremovableBeanBuildItem> producer) {
        producer.produce(UnremovableBeanBuildItem.beanTypes(MSSQLPoolCreator.class));
    }

    @BuildStep
    void validateBeans(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        // no two MssqlPoolCreator beans can be associated with the same datasource
        Map<String, Boolean> seen = new HashMap<>();
        for (BeanInfo beanInfo : validationPhase.getContext().beans()
                .matchBeanTypes(new MSSQLPoolCreatorBeanClassPredicate())) {
            Set<Name> qualifiers = new TreeSet<>(); // use a TreeSet in order to get a predictable iteration order
            for (AnnotationInstance qualifier : beanInfo.getQualifiers()) {
                qualifiers.add(Name.from(qualifier));
            }
            String qualifiersStr = qualifiers.stream().map(Name::toString).collect(Collectors.joining("_"));
            if (seen.getOrDefault(qualifiersStr, false)) {
                errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                        new IllegalStateException(
                                "There can be at most one bean of type '" + MSSQLPoolCreator.class.getName()
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
                            MsSQLServiceBindingConverter.class.getName()));
        }
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.MSSQL));
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

        healthChecks.produce(
                new HealthBuildItem("io.quarkus.reactive.mssql.client.runtime.health.ReactiveMSSQLDataSourcesHealthCheck",
                        dataSourcesBuildTimeConfig.healthEnabled()));
    }

    private void createPoolIfDefined(MSSQLPoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            BuildProducer<MSSQLPoolBuildItem> msSQLPool,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            String dataSourceName,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactiveMSSQLConfig dataSourcesReactiveMSSQLConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        if (!isReactiveMSSQLPoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig, dataSourceName,
                defaultDataSourceDbKindBuildItems, curateOutcomeBuildItem)) {
            return;
        }

        Function<SyntheticCreationalContext<MSSQLPool>, MSSQLPool> poolFunction = recorder.configureMSSQLPool(vertx.getVertx(),
                eventLoopCount.getEventLoopCount(),
                dataSourceName,
                dataSourcesRuntimeConfig,
                dataSourcesReactiveRuntimeConfig,
                dataSourcesReactiveMSSQLConfig,
                shutdown);
        msSQLPool.produce(new MSSQLPoolBuildItem(dataSourceName, poolFunction));

        ExtendedBeanConfigurator msSQLPoolBeanConfigurator = SyntheticBeanBuildItem.configure(MSSQLPool.class)
                .defaultBean()
                .addType(Pool.class)
                .scope(ApplicationScoped.class)
                .addInjectionPoint(POOL_INJECTION_TYPE, injectionPointAnnotations(dataSourceName))
                .addInjectionPoint(ClassType.create(DataSourceSupport.class))
                .createWith(poolFunction)
                .unremovable()
                .setRuntimeInit();

        addQualifiers(msSQLPoolBeanConfigurator, dataSourceName);

        syntheticBeans.produce(msSQLPoolBeanConfigurator.done());

        ExtendedBeanConfigurator mutinyMSSQLPoolConfigurator = SyntheticBeanBuildItem
                .configure(io.vertx.mutiny.mssqlclient.MSSQLPool.class)
                .defaultBean()
                .addType(io.vertx.mutiny.sqlclient.Pool.class)
                .scope(ApplicationScoped.class)
                .addInjectionPoint(POOL_INJECTION_TYPE, injectionPointAnnotations(dataSourceName))
                .addInjectionPoint(ClassType.create(DataSourceSupport.class))
                .createWith(recorder.mutinyMSSQLPool(poolFunction))
                .unremovable()
                .setRuntimeInit();

        addQualifiers(mutinyMSSQLPoolConfigurator, dataSourceName);

        syntheticBeans.produce(mutinyMSSQLPoolConfigurator.done());
    }

    private AnnotationInstance[] injectionPointAnnotations(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return EMPTY_ANNOTATIONS;
        }
        return new AnnotationInstance[] {
                AnnotationInstance.builder(REACTIVE_DATASOURCE).add("value", dataSourceName).build() };
    }

    private static boolean isReactiveMSSQLPoolDefined(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig, String dataSourceName,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        DataSourceBuildTimeConfig dataSourceBuildTimeConfig = dataSourcesBuildTimeConfig
                .dataSources().get(dataSourceName);
        DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig = dataSourcesReactiveBuildTimeConfig
                .getDataSourceReactiveBuildTimeConfig(dataSourceName);

        Optional<String> dbKind = DefaultDataSourceDbKindBuildItem.resolve(dataSourceBuildTimeConfig.dbKind(),
                defaultDataSourceDbKindBuildItems,
                !DataSourceUtil.isDefault(dataSourceName) || dataSourceBuildTimeConfig.devservices().enabled()
                        .orElse(!dataSourcesBuildTimeConfig.hasNamedDataSources()),
                curateOutcomeBuildItem);
        if (!dbKind.isPresent()) {
            return false;
        }

        if (!DatabaseKind.isMsSQL(dbKind.get())
                || !dataSourceReactiveBuildTimeConfig.enabled()) {
            return false;
        }

        return true;
    }

    private boolean hasPools(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (isReactiveMSSQLPoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig,
                DataSourceUtil.DEFAULT_DATASOURCE_NAME, defaultDataSourceDbKindBuildItems, curateOutcomeBuildItem)) {
            return true;
        }

        for (String dataSourceName : dataSourcesBuildTimeConfig.dataSources().keySet()) {
            if (isReactiveMSSQLPoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig,
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

    private static class MSSQLPoolCreatorBeanClassPredicate implements Predicate<Set<Type>> {
        private static final Type MSSQL_POOL_CREATOR = Type.create(DotName.createSimple(MSSQLPoolCreator.class.getName()),
                Type.Kind.CLASS);

        @Override
        public boolean test(Set<Type> types) {
            return types.contains(MSSQL_POOL_CREATOR);
        }
    }
}
