package io.quarkus.hibernate.reactive.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.configureProperties;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.configureSqlLoadScript;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.hasEntities;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.isHibernateValidatorPresent;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.jsonMapperKind;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.setDialectAndStorageEngine;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.xmlMapperKind;
import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import jakarta.persistence.PersistenceUnitTransactionType;

import org.hibernate.reactive.provider.impl.ReactiveIntegrator;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.RecorderBeanInitializedBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceProviderSetUpBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.customized.FormatMapperKind;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider;
import io.quarkus.hibernate.reactive.runtime.HibernateReactive;
import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;

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
    void reflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, REFLECTIVE_CONSTRUCTORS_NEEDED));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(HibernateReactiveRecorder recorder,
            JpaModelBuildItem jpaModel) {
        final boolean enableRx = hasEntities(jpaModel);
        recorder.callHibernateReactiveFeatureInit(enableRx);
    }

    @BuildStep
    public void buildReactivePersistenceUnit(
            HibernateOrmConfig hibernateOrmConfig, CombinedIndexBuildItem index,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            JpaModelBuildItem jpaModel,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {

        final boolean enableHR = hasEntities(jpaModel);
        if (!enableHR) {
            // we have to bail out early as we might not have a Vertx pool configuration
            LOG.warn("Hibernate Reactive is disabled because no JPA entities were found");
            return;
        }

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
        HibernateOrmConfigPersistenceUnit persistenceUnitConfig = hibernateOrmConfig.defaultPersistenceUnit();

        String datasourceNameFromConf = persistenceUnitConfig.datasource().orElse(DataSourceUtil.DEFAULT_DATASOURCE_NAME);

        DataSourceBuildTimeConfig defaultDataSourceBuildTimeConfig = dataSourcesBuildTimeConfig.dataSources()
                .get(DataSourceUtil.DEFAULT_DATASOURCE_NAME);

        DataSourceBuildTimeConfig dataSourceBuildTimeConfig = dataSourcesBuildTimeConfig.dataSources()
                .get(datasourceNameFromConf);

        Optional<String> datasourceName = Optional.of(datasourceNameFromConf);
        Optional<String> explicitDialect = hibernateOrmConfig.defaultPersistenceUnit().dialect().dialect();
        Optional<String> explicitDbMinVersion = defaultDataSourceBuildTimeConfig.dbVersion();
        Optional<String> dbKindOptional = DefaultDataSourceDbKindBuildItem.resolve(
                dataSourceBuildTimeConfig.dbKind(),
                defaultDataSourceDbKindBuildItems,
                defaultDataSourceBuildTimeConfig.devservices().enabled()
                        .orElse(!dataSourcesBuildTimeConfig.hasNamedDataSources()),
                curateOutcomeBuildItem);
        Optional<String> dbVersion = dataSourceBuildTimeConfig.dbVersion();

        if (dbKindOptional.isEmpty()) {
            throw new ConfigurationException(
                    "The datasource must be configured for Hibernate Reactive. Refer to https://quarkus.io/guides/datasource for guidance.",
                    Set.of("quarkus.datasource.db-kind", "quarkus.datasource.username",
                            "quarkus.datasource.password"));
        }

        // We only support Hibernate Reactive with a reactive data source, otherwise we don't configure the PU
        DataSourcesReactiveBuildTimeConfig.DataSourceReactiveOuterNamedBuildTimeConfig dataSourceReactiveBuildTimeConfig = dataSourcesReactiveBuildTimeConfig
                .dataSources().get(datasourceNameFromConf);

        if (dataSourceReactiveBuildTimeConfig == null || !dataSourceReactiveBuildTimeConfig.reactive().enabled()) {
            LOG.warn("Hibernate Reactive is disabled because the datasource is not reactive");
            return;
        }

        QuarkusPersistenceUnitDescriptor reactivePU = generateReactivePersistenceUnit(
                hibernateOrmConfig, index, persistenceUnitConfig, jpaModel,
                dbKindOptional, explicitDialect, explicitDbMinVersion, applicationArchivesBuildItem,
                launchMode.getLaunchMode(),
                systemProperties, nativeImageResources, hotDeploymentWatchedFiles, dbKindDialectBuildItems);

        Optional<FormatMapperKind> jsonMapper = jsonMapperKind(capabilities, hibernateOrmConfig.mapping().format().global());
        Optional<FormatMapperKind> xmlMapper = xmlMapperKind(capabilities, hibernateOrmConfig.mapping().format().global());
        jsonMapper.flatMap(FormatMapperKind::requiredBeanType)
                .ifPresent(type -> unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(type)));
        xmlMapper.flatMap(FormatMapperKind::requiredBeanType)
                .ifPresent(type -> unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(type)));

        //Some constant arguments to the following method:
        // - this is Reactive
        // - we don't support starting Hibernate Reactive from a persistence.xml
        // - we don't support Hibernate Envers with Hibernate Reactive
        persistenceUnitDescriptors.produce(new PersistenceUnitDescriptorBuildItem(reactivePU,
                new RecordedConfig(
                        datasourceName,
                        dbKindOptional,
                        dbVersion,
                        persistenceUnitConfig.dialect().dialect(),
                        io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy.NONE,
                        hibernateOrmConfig.database().ormCompatibilityVersion(),
                        hibernateOrmConfig.mapping().format().global(),
                        persistenceUnitConfig.unsupportedProperties()),
                null,
                jpaModel.getXmlMappings(reactivePU.getName()),
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
    private static QuarkusPersistenceUnitDescriptor generateReactivePersistenceUnit(
            HibernateOrmConfig hibernateOrmConfig, CombinedIndexBuildItem index,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            JpaModelBuildItem jpaModel,
            Optional<String> dbKindOptional,
            Optional<String> explicitDialect,
            Optional<String> explicitDbMinVersion,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {
        //we have no persistence.xml so we will create a default one
        final String persistenceUnitConfigName = DEFAULT_PERSISTENCE_UNIT_NAME;

        Map<String, Set<String>> modelClassesAndPackagesPerPersistencesUnits = HibernateOrmProcessor
                .getModelClassesAndPackagesPerPersistenceUnits(hibernateOrmConfig, jpaModel, index.getIndex(), true);
        Set<String> nonDefaultPUWithModelClassesOrPackages = modelClassesAndPackagesPerPersistencesUnits.entrySet().stream()
                .filter(e -> !DEFAULT_PERSISTENCE_UNIT_NAME.equals(e.getKey()) && !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!nonDefaultPUWithModelClassesOrPackages.isEmpty()) {
            // Not supported yet; see https://github.com/quarkusio/quarkus/issues/21110
            LOG.warnf("Entities are affected to non-default Hibernate Reactive persistence units %s."
                    + " Since Hibernate Reactive only works with the default persistence unit, those entities will be ignored.",
                    nonDefaultPUWithModelClassesOrPackages);
        }
        Set<String> modelClassesAndPackages = modelClassesAndPackagesPerPersistencesUnits
                .getOrDefault(persistenceUnitConfigName, Collections.emptySet());

        if (modelClassesAndPackages.isEmpty()) {
            LOG.warnf("Could not find any entities affected to the Hibernate Reactive persistence unit.");
        }

        QuarkusPersistenceUnitDescriptor descriptor = new QuarkusPersistenceUnitDescriptor(
                HibernateReactive.DEFAULT_REACTIVE_PERSISTENCE_UNIT_NAME, persistenceUnitConfigName,
                PersistenceUnitTransactionType.RESOURCE_LOCAL,
                new ArrayList<>(modelClassesAndPackages),
                new Properties(),
                true);

        Set<String> storageEngineCollector = new HashSet<>();

        setDialectAndStorageEngine(
                persistenceUnitConfigName,
                dbKindOptional,
                explicitDialect,
                explicitDbMinVersion,
                dbKindDialectBuildItems,
                persistenceUnitConfig.dialect().storageEngine(),
                systemProperties,
                descriptor.getProperties()::setProperty,
                storageEngineCollector);

        configureProperties(descriptor, persistenceUnitConfig, hibernateOrmConfig, true);
        configureSqlLoadScript(persistenceUnitConfigName, persistenceUnitConfig, applicationArchivesBuildItem, launchMode,
                nativeImageResources, hotDeploymentWatchedFiles, descriptor);

        return descriptor;
    }
}
