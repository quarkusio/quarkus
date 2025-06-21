package io.quarkus.reactive.mssql.client.deployment;

import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildUtil.qualifier;
import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildUtil.qualifiers;
import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceDotNames.INJECT_INSTANCE;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

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
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
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
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.reactive.mssql.client.MSSQLPoolCreator;
import io.quarkus.reactive.mssql.client.runtime.MSSQLPoolRecorder;
import io.quarkus.reactive.mssql.client.runtime.MSSQLPoolSupport;
import io.quarkus.reactive.mssql.client.runtime.MsSQLServiceBindingConverter;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.Pool;

class ReactiveMSSQLClientProcessor {

    private static final Type MSSQL_POOL_CREATOR = ClassType.create(DotName.createSimple(MSSQLPoolCreator.class.getName()));
    private static final ParameterizedType POOL_CREATOR_INJECTION_TYPE = ParameterizedType.create(INJECT_INSTANCE,
            new Type[] { MSSQL_POOL_CREATOR }, null);

    private static final DotName VERTX_MSSQL_POOL = DotName.createSimple(MSSQLPool.class);
    private static final Type VERTX_MSSQL_POOL_TYPE = ClassType.create(VERTX_MSSQL_POOL);

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
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_MSSQL_CLIENT));

        Stream.Builder<String> msSQLPoolNamesBuilder = Stream.builder();
        for (String dataSourceName : dataSourcesBuildTimeConfig.dataSources().keySet()) {

            if (!isReactiveMSSQLPoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig, dataSourceName,
                    defaultDataSourceDbKindBuildItems, curateOutcomeBuildItem)) {
                continue;
            }

            createPool(recorder, vertx, eventLoopCount, shutdown, msSQLPool, syntheticBeans, dataSourceName);

            msSQLPoolNamesBuilder.add(dataSourceName);
        }

        Set<String> msSQLPoolNames = msSQLPoolNamesBuilder.build().collect(toSet());
        if (!msSQLPoolNames.isEmpty()) {
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(MSSQLPoolSupport.class)
                    .scope(Singleton.class)
                    .unremovable()
                    .runtimeValue(recorder.createMSSQLPoolSupport(msSQLPoolNames))
                    .setRuntimeInit()
                    .done());
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

    private void createPool(MSSQLPoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            BuildProducer<MSSQLPoolBuildItem> msSQLPool,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            String dataSourceName) {

        Function<SyntheticCreationalContext<MSSQLPool>, MSSQLPool> poolFunction = recorder.configureMSSQLPool(vertx.getVertx(),
                eventLoopCount.getEventLoopCount(), dataSourceName, shutdown);
        msSQLPool.produce(new MSSQLPoolBuildItem(dataSourceName, poolFunction));

        ExtendedBeanConfigurator msSQLPoolBeanConfigurator = SyntheticBeanBuildItem.configure(MSSQLPool.class)
                .defaultBean()
                .addType(Pool.class)
                .scope(ApplicationScoped.class)
                .qualifiers(qualifiers(dataSourceName))
                .addInjectionPoint(POOL_CREATOR_INJECTION_TYPE, qualifier(dataSourceName))
                .checkActive(recorder.poolCheckActiveSupplier(dataSourceName))
                .createWith(poolFunction)
                .unremovable()
                .setRuntimeInit()
                .startup();

        syntheticBeans.produce(msSQLPoolBeanConfigurator.done());

        ExtendedBeanConfigurator mutinyMSSQLPoolConfigurator = SyntheticBeanBuildItem
                .configure(io.vertx.mutiny.mssqlclient.MSSQLPool.class)
                .defaultBean()
                .addType(io.vertx.mutiny.sqlclient.Pool.class)
                .scope(ApplicationScoped.class)
                .qualifiers(qualifiers(dataSourceName))
                .addInjectionPoint(VERTX_MSSQL_POOL_TYPE, qualifier(dataSourceName))
                .checkActive(recorder.poolCheckActiveSupplier(dataSourceName))
                .createWith(recorder.mutinyMSSQLPool(dataSourceName))
                .unremovable()
                .setRuntimeInit()
                .startup();

        syntheticBeans.produce(mutinyMSSQLPoolConfigurator.done());
    }

    private static boolean isReactiveMSSQLPoolDefined(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig, String dataSourceName,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        DataSourceBuildTimeConfig dataSourceBuildTimeConfig = dataSourcesBuildTimeConfig
                .dataSources().get(dataSourceName);
        DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig = dataSourcesReactiveBuildTimeConfig
                .dataSources().get(dataSourceName).reactive();

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

    private static class MSSQLPoolCreatorBeanClassPredicate implements Predicate<Set<Type>> {

        @Override
        public boolean test(Set<Type> types) {
            return types.contains(MSSQL_POOL_CREATOR);
        }
    }
}
