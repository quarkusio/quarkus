package io.quarkus.hibernate.reactive.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.configureProperties;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.configureSqlLoadScript;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.isHibernateValidatorPresent;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.jsonFormatterCustomizationCheck;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.jsonMapperKind;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.setDialectAndStorageEngine;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.xmlMapperKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import jakarta.persistence.PersistenceUnitTransactionType;

import org.hibernate.reactive.provider.impl.ReactiveIntegrator;
import org.jboss.jandex.AnnotationValue;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryInjectionPointsBuildItem;
import io.quarkus.arc.deployment.InjectionPointScanningUtil;
import io.quarkus.arc.deployment.RecorderBeanInitializedBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DataSourceRequestBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.JpaModelPerPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaPersistenceUnitModel;
import io.quarkus.hibernate.orm.deployment.PersistenceProviderSetUpBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDefinitionBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.PersistenceUnitLookupBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.PersistenceUnitRequestBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.SqlLoadScriptDefaultBuildItem;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.customized.FormatMapperKind;
import io.quarkus.hibernate.orm.runtime.customized.JsonFormatterCustomizationCheck;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider;
import io.quarkus.hibernate.reactive.runtime.HibernateReactivePersistenceUnitProviderHelper;
import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.hibernate.reactive.runtime.transaction.HibernateActionsStrategy;
import io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildItem;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

@BuildSteps(onlyIf = HibernateReactiveEnabled.class)
public final class HibernateReactiveProcessor {

    private static final String HIBERNATE_REACTIVE = "Hibernate Reactive";
    private static final Logger LOG = Logger.getLogger(HibernateReactiveProcessor.class);
    static final String[] REFLECTIVE_CONSTRUCTORS_NEEDED = {
            "org.hibernate.reactive.persister.entity.impl.ReactiveSingleTableEntityPersister",
            "org.hibernate.reactive.persister.entity.impl.ReactiveJoinedSubclassEntityPersister",
            "org.hibernate.reactive.persister.entity.impl.ReactiveUnionSubclassEntityPersister",
            "org.hibernate.reactive.persister.collection.impl.ReactiveOneToManyPersister",
            "org.hibernate.reactive.persister.collection.impl.ReactiveBasicCollectionPersister",
    };

    @BuildStep
    void registerServicesForReflection(BuildProducer<ServiceProviderBuildItem> services) {
        services.produce(new ServiceProviderBuildItem(
                "io.vertx.core.spi.VertxServiceProvider",
                "org.hibernate.reactive.context.impl.ContextualDataStorage"));
    }

    @BuildStep
    AdditionalBeanBuildItem produceDatabaseActionsStrategy() {
        return AdditionalBeanBuildItem.unremovableOf(HibernateActionsStrategy.class);
    }

    @BuildStep
    void reflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(REFLECTIVE_CONSTRUCTORS_NEEDED).build());
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(HibernateReactiveRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> descriptors) {
        final boolean enableRx = !descriptors.isEmpty();
        recorder.callHibernateReactiveFeatureInit(enableRx);
    }

    @BuildStep
    void collectImplicitReactivePersistenceUnitRequests(HibernateOrmConfig config,
            JpaModelPerPersistenceUnitBuildItem jpaModelPerPersistenceUnit,
            PersistenceUnitLookupBuildItem lookupBuildItem,
            BuildProducer<PersistenceUnitRequestBuildItem> puRequests) {
        HibernateProcessorUtil.collectPersistenceUnitRequestsFromConfiguration(ProgrammingParadigm.REACTIVE, config,
                jpaModelPerPersistenceUnit, lookupBuildItem, puRequests);

    }

    @BuildStep
    void collectReactivePersistenceUnitRequestsFromInjection(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            BeanDiscoveryInjectionPointsBuildItem injectionPointIndex,
            BuildProducer<PersistenceUnitRequestBuildItem> puRequests) {
        InjectionPointScanningUtil.collectUnsatisfiedInjectionPoints(
                beanDiscovery, injectionPointIndex,
                HibernateReactiveCdiProcessor.ALL_REACTIVE_INJECTABLE_TYPES,
                List.of(io.quarkus.hibernate.orm.deployment.ClassNames.QUARKUS_PERSISTENCE_UNIT, DotNames.NAMED),
                PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                qualifier -> {
                    AnnotationValue value = qualifier.value();
                    return (value != null && !value.asString().isEmpty()) ? value.asString()
                            : PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
                },
                (name, reason) -> puRequests
                        .produce(new PersistenceUnitRequestBuildItem(name, ProgrammingParadigm.REACTIVE, reason)));
    }

    @BuildStep
    void definePersistenceUnits(
            HibernateOrmConfig hibernateOrmConfig,
            PersistenceUnitLookupBuildItem lookupBuildItem,
            List<PersistenceUnitRequestBuildItem> puRequests,
            BuildProducer<PersistenceUnitDefinitionBuildItem> persistenceUnitDefinitions) {
        HibernateProcessorUtil.definePersistenceUnits(ProgrammingParadigm.REACTIVE, hibernateOrmConfig, lookupBuildItem,
                puRequests, persistenceUnitDefinitions);
    }

    @BuildStep
    public void collectDatasourceReferencesFromPersistenceUnits(
            List<PersistenceUnitDefinitionBuildItem> puDefinitions,
            BuildProducer<DataSourceRequestBuildItem> datasourceReferences) {
        for (PersistenceUnitDefinitionBuildItem puDefinition : puDefinitions) {
            if (!ProgrammingParadigm.REACTIVE.equals(puDefinition.getParadigm())
                    || puDefinition.getDataSourceName().isEmpty()) {
                continue;
            }
            Reason reason = new Reason(
                    "Hibernate Reactive persistence unit '" + puDefinition.getPersistenceUnitName() + "'",
                    puDefinition.getReasons());
            datasourceReferences.produce(new DataSourceRequestBuildItem(puDefinition.getDataSourceName().get(),
                    ProgrammingParadigm.REACTIVE, reason));
        }
    }

    @BuildStep
    public void buildReactivePersistenceUnitsFromConfig(
            HibernateOrmConfig hibernateOrmConfig,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            List<PersistenceUnitDefinitionBuildItem> persistenceUnitDefinitions,
            List<ReactiveDataSourceBuildItem> reactiveDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            JpaModelPerPersistenceUnitBuildItem jpaModel,
            Capabilities capabilities,
            List<SqlLoadScriptDefaultBuildItem> additionalSqlLoadScriptDefaults,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {
        // Block any reactive persistence units from using persistence.xml
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptorBuildItem : persistenceXmlDescriptors) {
            String provider = persistenceXmlDescriptorBuildItem.getDescriptor().getProviderClassName();
            if (provider == null ||
                    provider.equals(FastBootHibernateReactivePersistenceProvider.class.getCanonicalName()) ||
                    provider.equals(FastBootHibernateReactivePersistenceProvider.IMPLEMENTATION_NAME)) {
                throw new ConfigurationException(
                        "Cannot use persistence.xml with Hibernate Reactive in Quarkus. Must use application.properties instead.");
            }
        }

        for (PersistenceUnitDefinitionBuildItem puDefinition : persistenceUnitDefinitions) {
            if (puDefinition.getParadigm() != ProgrammingParadigm.REACTIVE) {
                continue;
            }
            String puName = puDefinition.getPersistenceUnitName();
            var model = jpaModel.getModelPerPersistenceUnit().get(puName);
            if (model == null) {
                model = new JpaPersistenceUnitModel();
            }

            buildReactivePersistenceUnitFromConfig(hibernateOrmConfig, puDefinition, model,
                    reactiveDataSources,
                    applicationArchivesBuildItem,
                    launchMode,
                    capabilities,
                    additionalSqlLoadScriptDefaults,
                    nativeImageResources,
                    hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    unremovableBeans, dbKindDialectBuildItems);
        }
    }

    private static void buildReactivePersistenceUnitFromConfig(HibernateOrmConfig hibernateOrmConfig,
            PersistenceUnitDefinitionBuildItem puDefinition,
            JpaPersistenceUnitModel model,
            List<ReactiveDataSourceBuildItem> reactiveDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            Capabilities capabilities,
            List<SqlLoadScriptDefaultBuildItem> additionalSqlLoadScriptDefaults,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {
        String persistenceUnitName = puDefinition.getPersistenceUnitName();
        HibernateOrmConfigPersistenceUnit persistenceUnitConfig = puDefinition.getConfig();// Reactive does not support multitenancy so we always require a datasource (explicit or implied),
        // so this optional should have previously been checked and should be non-empty.
        // See https://github.com/quarkusio/quarkus/issues/15959
        String dataSourceName = puDefinition.getDataSourceName().orElseThrow();
        ReactiveDataSourceBuildItem reactiveDataSource = HibernateProcessorUtil.findDataSourceWithName(
                dataSourceName,
                reactiveDataSources,
                ReactiveDataSourceBuildItem::getName);

        Optional<String> explicitDialect = persistenceUnitConfig.dialect().dialect();
        Optional<String> dbVersion = reactiveDataSource.getVersion();
        Optional<String> dbKindOptional = Optional.of(reactiveDataSource.getDbKind());

        QuarkusPersistenceUnitDescriptorWithSupportedDBKind reactivePUWithDBKind = generateReactivePersistenceUnit(
                hibernateOrmConfig, persistenceUnitName, persistenceUnitConfig, model,
                dbKindOptional, explicitDialect, dbVersion, applicationArchivesBuildItem,
                launchMode.getLaunchMode(),
                additionalSqlLoadScriptDefaults,
                nativeImageResources, hotDeploymentWatchedFiles, dbKindDialectBuildItems);

        Optional<FormatMapperKind> jsonMapper = jsonMapperKind(capabilities, hibernateOrmConfig.mapping().format().global());
        Optional<FormatMapperKind> xmlMapper = xmlMapperKind(capabilities, hibernateOrmConfig.mapping().format().global());
        jsonMapper.flatMap(FormatMapperKind::requiredBeanType)
                .ifPresent(type -> unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(type)));
        xmlMapper.flatMap(FormatMapperKind::requiredBeanType)
                .ifPresent(type -> unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(type)));
        JsonFormatterCustomizationCheck jsonFormatterCustomizationCheck = jsonFormatterCustomizationCheck(capabilities,
                jsonMapper);

        QuarkusPersistenceUnitDescriptor reactivePU = reactivePUWithDBKind.descriptor();
        Set<String> entityClassNames = new HashSet<>(reactivePU.getManagedClassNames());
        entityClassNames.retainAll(model.entityClassNames());

        //Some constant arguments to the following method:
        // - this is Reactive
        // - we don't support starting Hibernate Reactive from a persistence.xml
        // - we don't support Hibernate Envers with Hibernate Reactive
        persistenceUnitDescriptors.produce(new PersistenceUnitDescriptorBuildItem(reactivePU,
                new RecordedConfig(
                        Optional.of(dataSourceName),
                        dbKindOptional,
                        reactivePUWithDBKind.supportedDatabaseKind.map(DatabaseKind.SupportedDatabaseKind::getMainName),
                        dbVersion,
                        persistenceUnitConfig.dialect().dialect(),
                        entityClassNames,
                        io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy.NONE,
                        hibernateOrmConfig.database().ormCompatibilityVersion(),
                        hibernateOrmConfig.mapping().format().global(),
                        jsonFormatterCustomizationCheck,
                        persistenceUnitConfig.unsupportedProperties()),
                null,
                model.xmlMappings(),
                false,
                isHibernateValidatorPresent(capabilities), jsonMapper, xmlMapper));

    }

    @BuildStep
    @Consume(VertxPoolBuildItem.class)
    void waitForVertxPool(List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            // Define a dependency on VertxPoolBuildItem to ensure that any Pool instances are available
            // when HibernateORM starts its persistence units
            runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_REACTIVE,
                    puDescriptor.getPersistenceUnitName()));
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    PersistenceProviderSetUpBuildItem setUpPersistenceProviderAndWaitForVertxPool(HibernateReactiveRecorder recorder,
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> integrationBuildItems,
            BuildProducer<RecorderBeanInitializedBuildItem> orderEnforcer) {
        recorder.initializePersistenceProvider(
                HibernateOrmIntegrationRuntimeConfiguredBuildItem.collectDescriptors(integrationBuildItems));
        return new PersistenceProviderSetUpBuildItem();
    }

    @BuildStep
    void silenceLogging(BuildProducer<LogCategoryBuildItem> logCategories) {
        logCategories.produce(new LogCategoryBuildItem(ReactiveIntegrator.class.getName(), Level.WARNING));
    }

    record QuarkusPersistenceUnitDescriptorWithSupportedDBKind(QuarkusPersistenceUnitDescriptor descriptor,
            Optional<DatabaseKind.SupportedDatabaseKind> supportedDatabaseKind) {
    }

    /**
     * This is mostly copied from
     * io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor#handleHibernateORMWithNoPersistenceXml
     * Key differences are:
     * - Always produces a persistence unit descriptor, since we assume there always 1 reactive persistence unit
     * - Any JDBC-only configuration settings are removed
     * - If we ever add any Reactive-only config settings, they can be set here
     */
    // TODO this whole method is really just a hack that duplicates
    //  io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor.handleHibernateORMWithNoPersistenceXml
    //  and customizes it for Hibernate Reactive.
    //  we should work on a way to merge the two methods while still having some behavior specific to
    //  HR/ORM, because it's likely the HR implementation is missing some features,
    //  and we've seen in the past that features we add to handleHibernateORMWithNoPersistenceXml
    //  tend not to be added here.
    //  See https://github.com/quarkusio/quarkus/issues/28629.
    //see producePersistenceUnitDescriptorFromConfig in ORM
    private static QuarkusPersistenceUnitDescriptorWithSupportedDBKind generateReactivePersistenceUnit(
            HibernateOrmConfig hibernateOrmConfig,
            String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            JpaPersistenceUnitModel model,
            Optional<String> dbKindOptional,
            Optional<String> explicitDialect,
            Optional<String> explicitDbMinVersion,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            List<SqlLoadScriptDefaultBuildItem> additionalSqlLoadScriptDefaults,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {
        QuarkusPersistenceUnitDescriptor descriptor = new QuarkusPersistenceUnitDescriptor(
                persistenceUnitName,
                new HibernateReactivePersistenceUnitProviderHelper(),
                PersistenceUnitTransactionType.RESOURCE_LOCAL,
                new ArrayList<>(model == null ? Collections.emptySet() : model.allModelClassAndPackageNames()),
                new Properties(),
                true);

        HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDialect dialectConfig = persistenceUnitConfig
                .dialect();
        Optional<DatabaseKind.SupportedDatabaseKind> supportedDatabaseKind = setDialectAndStorageEngine(
                persistenceUnitName,
                dbKindOptional,
                explicitDialect,
                explicitDbMinVersion,
                dialectConfig,
                dbKindDialectBuildItems,
                descriptor.getProperties()::setProperty);

        configureProperties(descriptor, persistenceUnitConfig, hibernateOrmConfig, true);
        configureSqlLoadScript(persistenceUnitName, persistenceUnitConfig, applicationArchivesBuildItem, launchMode,
                additionalSqlLoadScriptDefaults,
                nativeImageResources, hotDeploymentWatchedFiles, descriptor);

        return new QuarkusPersistenceUnitDescriptorWithSupportedDBKind(descriptor, supportedDatabaseKind);
    }
}
