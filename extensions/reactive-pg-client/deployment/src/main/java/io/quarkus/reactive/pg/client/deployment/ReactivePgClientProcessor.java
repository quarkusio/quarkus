package io.quarkus.reactive.pg.client.deployment;

import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildUtil.qualifier;
import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildUtil.qualifiers;
import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceDotNames.INJECT_INSTANCE;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
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
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.spi.ReactiveDataSourceInjectableTypeBuildItem;
import io.quarkus.reactive.pg.client.PgPoolCreator;
import io.quarkus.reactive.pg.client.runtime.PgPoolRecorder;
import io.quarkus.reactive.pg.client.runtime.PgPoolSupport;
import io.quarkus.reactive.pg.client.runtime.PostgreSQLServiceBindingConverter;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.spi.PgDriver;
import io.vertx.sqlclient.Pool;

class ReactivePgClientProcessor {

    private static final Type PG_POOL_CREATOR = ClassType.create(DotName.createSimple(PgPoolCreator.class.getName()));
    private static final ParameterizedType POOL_CREATOR_INJECTION_TYPE = ParameterizedType.create(INJECT_INSTANCE,
            new Type[] { PG_POOL_CREATOR }, null);

    private static final DotName VERTX_PG_POOL = DotName.createSimple(PgPool.class);
    private static final Type VERTX_PG_POOL_TYPE = ClassType.create(VERTX_PG_POOL);

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
    void registerInjectableTypes(BuildProducer<ReactiveDataSourceInjectableTypeBuildItem> producer) {
        producer.produce(new ReactiveDataSourceInjectableTypeBuildItem(VERTX_PG_POOL));
        producer.produce(
                new ReactiveDataSourceInjectableTypeBuildItem(DotName.createSimple(io.vertx.mutiny.pgclient.PgPool.class)));
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
            List<DataSourceDefinedBuildItem> definedDataSources) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_PG_CLIENT));

        Stream.Builder<String> pgPoolNamesBuilder = Stream.builder();
        for (DataSourceDefinedBuildItem dataSource : definedDataSources) {
            if (dataSource.getParadigm() != ProgrammingParadigm.REACTIVE
                    || !DatabaseKind.isPostgreSQL(dataSource.getDbKind())) {
                continue;
            }
            String dataSourceName = dataSource.getName();

            createPool(recorder, vertx, eventLoopCount, shutdown, pgPool, syntheticBeans, dataSourceName);

            pgPoolNamesBuilder.add(dataSourceName);
        }

        Set<String> pgPoolNames = pgPoolNamesBuilder.build().collect(toSet());
        if (!pgPoolNames.isEmpty()) {
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(PgPoolSupport.class)
                    .scope(Singleton.class)
                    .unremovable()
                    .runtimeValue(recorder.createPgPoolSupport(pgPoolNames))
                    .setRuntimeInit()
                    .done());
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_PG_CLIENT));

        vertxPool.produce(new VertxPoolBuildItem());
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
            List<DataSourceDefinedBuildItem> definedDataSources) {
        if (!capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return;
        }

        if (!hasPools(definedDataSources)) {
            return;
        }

        healthChecks
                .produce(new HealthBuildItem("io.quarkus.reactive.pg.client.runtime.health.ReactivePgDataSourcesHealthCheck",
                        dataSourcesBuildTimeConfig.healthEnabled()));
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
    void registerDriver(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem("io.vertx.sqlclient.spi.Driver", PgDriver.class.getName()));
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

    private void createPool(PgPoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            BuildProducer<PgPoolBuildItem> pgPool,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            String dataSourceName) {

        Function<SyntheticCreationalContext<PgPool>, PgPool> poolFunction = recorder.configurePgPool(vertx.getVertx(),
                eventLoopCount.getEventLoopCount(), dataSourceName, shutdown);
        pgPool.produce(new PgPoolBuildItem(dataSourceName, poolFunction));

        ExtendedBeanConfigurator pgPoolBeanConfigurator = SyntheticBeanBuildItem.configure(PgPool.class)
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

        syntheticBeans.produce(pgPoolBeanConfigurator.done());

        // the Mutiny pool is created by using the Vertx pool
        ExtendedBeanConfigurator mutinyPgPoolConfigurator = SyntheticBeanBuildItem
                .configure(io.vertx.mutiny.pgclient.PgPool.class)
                .defaultBean()
                .addType(io.vertx.mutiny.sqlclient.Pool.class)
                .scope(ApplicationScoped.class)
                .qualifiers(qualifiers(dataSourceName))
                .addInjectionPoint(VERTX_PG_POOL_TYPE, qualifier(dataSourceName))
                .checkActive(recorder.poolCheckActiveSupplier(dataSourceName))
                .createWith(recorder.mutinyPgPool(dataSourceName))
                .unremovable()
                .setRuntimeInit()
                .startup();

        syntheticBeans.produce(mutinyPgPoolConfigurator.done());
    }

    private static boolean hasPools(List<DataSourceDefinedBuildItem> definedDataSources) {
        return definedDataSources.stream()
                .anyMatch(ds -> ds.getParadigm() == ProgrammingParadigm.REACTIVE
                        && DatabaseKind.isPostgreSQL(ds.getDbKind()));
    }

    private static class PgPoolCreatorBeanClassPredicate implements Predicate<Set<Type>> {

        @Override
        public boolean test(Set<Type> types) {
            return types.contains(PG_POOL_CREATOR);
        }
    }
}
