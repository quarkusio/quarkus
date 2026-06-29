package io.quarkus.reactive.datasource.deployment;

import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildUtil.qualifier;
import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildUtil.qualifiers;
import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceDotNames.INJECT_INSTANCE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryInjectionPointsBuildItem;
import io.quarkus.arc.deployment.InjectionPointScanningUtil;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.devui.Name;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.DataSourceProcessorUtil;
import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceDefinitionBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestHandlerBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.ReactivePoolRecorder;
import io.quarkus.reactive.datasource.runtime.ReactivePoolsHealthConfig;
import io.quarkus.reactive.datasource.spi.ReactivePoolBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.vertx.sqlclient.Pool;

class ReactiveDataSourceProcessor {
    private static final Logger log = Logger.getLogger(ReactiveDataSourceProcessor.class);

    private static final Type POOL_CREATOR_TYPE = ClassType.create(DotName.createSimple(PoolCreator.class));
    private static final ParameterizedType POOL_CREATOR_INJECTION_TYPE = ParameterizedType.create(INJECT_INSTANCE,
            new Type[] { POOL_CREATOR_TYPE }, null);
    private static final DotName POOL = DotName.createSimple(Pool.class);
    private static final Type POOL_TYPE = ClassType.create(POOL);

    private static final DotName REACTIVE_DATASOURCE_QUALIFIER = DotName.createSimple(ReactiveDataSource.class);

    @BuildStep
    DataSourceRequestHandlerBuildItem defineDataSourceRequestHandler(
            DataSourcesReactiveBuildTimeConfig reactiveConfig,
            DataSourceDbKindResolverBuildItem dbKindResolverBuildItem) {
        var dbKindResolver = dbKindResolverBuildItem.get();
        return new DataSourceRequestHandlerBuildItem(ProgrammingParadigm.REACTIVE, dataSourceName -> {
            var unavailableReasons = new ArrayList<Reason>();
            if (!reactiveConfig.dataSources().get(dataSourceName).reactive().enabled()) {
                unavailableReasons.add(new Reason(String.format(Locale.ROOT, """
                        Reactive datasource '%s' was disabled explicitly by setting '%s' to 'false'. \
                        Refer to https://quarkus.io/guides/datasource for guidance.
                        """,
                        dataSourceName,
                        DataSourceUtil.dataSourcePropertyKey(dataSourceName, "reactive"))));
            }
            if (dbKindResolver.getOptional(dataSourceName).isEmpty()) {
                unavailableReasons.add(dbKindResolver.unavailableReason(dataSourceName, ProgrammingParadigm.REACTIVE));
            }
            return unavailableReasons;
        });
    }

    @BuildStep
    void collectImplicitReactiveDataSourceRequests(
            DataSourcesBuildTimeConfig config,
            DataSourcesReactiveBuildTimeConfig reactiveConfig,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        Predicate<String> enabled = name -> reactiveConfig.dataSources().get(name).reactive().enabled();
        DataSourceProcessorUtil.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.REACTIVE, config, config.dataSources().keySet(), enabled,
                "*", dataSourceRequests);
        DataSourceProcessorUtil.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.REACTIVE, config, reactiveConfig.dataSources().keySet(), enabled,
                "reactive.*", dataSourceRequests);

    }

    @BuildStep
    void collectReactiveDataSourceRequestsFromInjection(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            BeanDiscoveryInjectionPointsBuildItem injectionPointIndex,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        Set<DotName> reactiveTypes = new HashSet<>();
        reactiveTypes.add(ReactiveDataSourceDotNames.VERTX_POOL);
        reactiveTypes.add(ReactiveDataSourceDotNames.MUTINY_POOL);

        InjectionPointScanningUtil.collectUnsatisfiedInjectionPoints(
                beanDiscovery, injectionPointIndex,
                reactiveTypes,
                List.of(REACTIVE_DATASOURCE_QUALIFIER, DotNames.NAMED),
                DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                qualifier -> {
                    AnnotationValue value = qualifier.value();
                    return (value != null && !value.asString().isEmpty()) ? value.asString()
                            : DataSourceUtil.DEFAULT_DATASOURCE_NAME;
                },
                (name, reason) -> dataSourceRequests
                        .produce(new DataSourceRequestBuildItem(name, ProgrammingParadigm.REACTIVE, reason)));
    }

    @BuildStep
    public void defineReactiveDataSources(
            DataSourcesBuildTimeConfig config,
            DataSourcesReactiveBuildTimeConfig reactiveConfig,
            DataSourceDbKindResolverBuildItem dbKindResolutionBuildItem,
            DataSourceLookupBuildItem lookupBuildItem,
            List<DataSourceRequestBuildItem> dataSourceReferences,
            BuildProducer<ReactiveDataSourceDefinitionBuildItem> dataSourceDefinitions,
            BuildProducer<DataSourceDefinitionBuildItem> definedDataSources,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        Set<String> defined = DataSourceProcessorUtil.defineDataSources(
                ProgrammingParadigm.REACTIVE, config,
                lookupBuildItem,
                dataSourceReferences,
                validationErrors);

        if (defined.isEmpty()) {
            log.warn("The Datasource Reactive dependency is present but no Reactive datasources have been defined.");
            return;
        }

        for (String dataSourceName : defined) {
            String dbKind = dbKindResolutionBuildItem.get().getOptional(dataSourceName)
                    // Should not throw since DataSourceProcessorUtil.defineDataSources skips datasources with no db-kind.
                    .orElseThrow();

            definedDataSources.produce(new DataSourceDefinitionBuildItem(dataSourceName, dbKind, ProgrammingParadigm.REACTIVE));

            dataSourceDefinitions.produce(new ReactiveDataSourceDefinitionBuildItem(dataSourceName,
                    config.dataSources().get(dataSourceName),
                    reactiveConfig.dataSources().get(dataSourceName).reactive(),
                    dbKind));
        }
    }

    @BuildStep
    void addQualifierAsBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // add the @ReactiveDataSource class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ReactiveDataSource.class).build());
    }

    @BuildStep
    void produceReactiveDataSourceBuildItem(
            List<ReactiveDataSourceDefinitionBuildItem> dataSourceDefinitions,
            BuildProducer<io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem> dataSource) {
        if (dataSourceDefinitions.isEmpty()) {
            return;
        }

        for (ReactiveDataSourceDefinitionBuildItem dsDefinition : dataSourceDefinitions) {
            dataSource.produce(new io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem(
                    dsDefinition.getName(),
                    dsDefinition.getDbKind(),
                    dsDefinition.isDefault(),
                    dsDefinition.getDataSourceConfig().dbVersion()));
        }
    }

    @BuildStep
    void convertSPIReactiveDataSourceToDeprecatedOne(
            List<io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem> dataSource,
            BuildProducer<ReactiveDataSourceBuildItem> reactiveDataSource) {

        for (io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem newDataSourceBuildItem : dataSource) {
            reactiveDataSource.produce(new ReactiveDataSourceBuildItem(
                    newDataSourceBuildItem.getName(),
                    newDataSourceBuildItem.getDbKind(),
                    newDataSourceBuildItem.isDefault(),
                    newDataSourceBuildItem.getVersion()));
        }
    }

    /**
     * Consume {@link ReactivePoolBuildItem}s produced by each DB-specific extension (PG, MySQL, etc.)
     * and register generic synthetic CDI beans for {@link Pool} and Mutiny Pool.
     * Also produces a {@link VertxPoolBuildItem} for backwards compatibility with Hibernate Reactive.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerReactivePools(
            ReactivePoolRecorder recorder,
            List<ReactivePoolBuildItem> poolBuildItems,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<VertxPoolBuildItem> vertxPool) {

        if (poolBuildItems.isEmpty()) {
            return;
        }

        Map<String, String> healthSqlMap = new HashMap<>();

        for (ReactivePoolBuildItem poolBuildItem : poolBuildItems) {
            String dataSourceName = poolBuildItem.getDataSourceName();

            // Register the bare Vert.x Pool as a synthetic bean
            ExtendedBeanConfigurator poolBeanConfigurator = SyntheticBeanBuildItem.configure(Pool.class)
                    .defaultBean()
                    .addType(Pool.class)
                    .scope(ApplicationScoped.class)
                    .qualifiers(qualifiers(dataSourceName))
                    .addInjectionPoint(POOL_CREATOR_INJECTION_TYPE, qualifier(dataSourceName))
                    .checkActive(recorder.poolCheckActiveSupplier(dataSourceName))
                    .createWith(poolBuildItem.getPool())
                    .unremovable()
                    .setRuntimeInit()
                    .startup();
            syntheticBeans.produce(poolBeanConfigurator.done());

            // Register the Mutiny Pool as a synthetic bean
            ExtendedBeanConfigurator mutinyPoolConfigurator = SyntheticBeanBuildItem
                    .configure(io.vertx.mutiny.sqlclient.Pool.class)
                    .defaultBean()
                    .addType(io.vertx.mutiny.sqlclient.Pool.class)
                    .scope(ApplicationScoped.class)
                    .qualifiers(qualifiers(dataSourceName))
                    .addInjectionPoint(POOL_TYPE, qualifier(dataSourceName))
                    .checkActive(recorder.poolCheckActiveSupplier(dataSourceName))
                    .createWith(recorder.mutinyPool(dataSourceName))
                    .unremovable()
                    .setRuntimeInit()
                    .startup();
            syntheticBeans.produce(mutinyPoolConfigurator.done());

            healthSqlMap.put(dataSourceName, poolBuildItem.getHealthCheckSql());
        }

        // Register the health config bean
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(ReactivePoolsHealthConfig.class)
                .scope(Singleton.class)
                .unremovable()
                .runtimeValue(recorder.createHealthConfig(healthSqlMap))
                .setRuntimeInit()
                .done());

        // Produce VertxPoolBuildItem for backwards compatibility (Hibernate Reactive consumes it)
        vertxPool.produce(new VertxPoolBuildItem());
    }

    /**
     * The health check needs to be produced in a separate method to avoid a circular dependency
     * (the Vert.x instance creation consumes the AdditionalBeanBuildItems).
     * We intentionally avoid consuming ReactiveDataSourceDefinitionBuildItem or
     * DataSourceDefinedBuildItem here, because they depend (indirectly) on
     * BeanDiscoveryFinishedBuildItem, which would create a cycle through
     * HealthBuildItem → AdditionalBeanBuildItem → Arc.
     * Since this build step is in ReactiveDataSourceProcessor, which is only
     * loaded when the reactive datasource extension is present, we always
     * register the health check.
     */
    @BuildStep
    void addHealthCheck(
            Capabilities capabilities,
            BuildProducer<HealthBuildItem> healthChecks,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        if (!capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return;
        }
        healthChecks.produce(new HealthBuildItem(
                "io.quarkus.reactive.datasource.runtime.ReactiveDataSourcesHealthCheck",
                dataSourcesBuildTimeConfig.healthEnabled()));
    }

    @BuildStep
    void unremoveableBeans(BuildProducer<UnremovableBeanBuildItem> producer) {
        producer.produce(UnremovableBeanBuildItem.beanTypes(PoolCreator.class));
    }

    @BuildStep
    void validateBeans(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        // no two PoolCreator beans can be associated with the same datasource
        Set<String> seen = new HashSet<>();
        for (BeanInfo beanInfo : validationPhase.getContext().beans()
                .matchBeanTypes(new PoolCreatorBeanClassPredicate())) {
            Set<Name> qualifiers = new TreeSet<>();
            for (AnnotationInstance q : beanInfo.getQualifiers()) {
                qualifiers.add(Name.from(q));
            }
            String qualifiersStr = qualifiers.stream().map(Name::toString).collect(Collectors.joining("_"));
            if (!seen.add(qualifiersStr)) {
                errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                        new IllegalStateException(
                                "There can be at most one bean of type '" + PoolCreator.class.getName()
                                        + "' for each datasource.")));
            }
        }
    }

    private static class PoolCreatorBeanClassPredicate implements Predicate<Set<Type>> {
        @Override
        public boolean test(Set<Type> types) {
            return types.contains(POOL_CREATOR_TYPE);
        }
    }
}
