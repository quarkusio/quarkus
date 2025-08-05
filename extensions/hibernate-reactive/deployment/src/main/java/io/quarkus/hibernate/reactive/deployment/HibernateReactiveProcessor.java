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
import java.util.function.Function;
import java.util.logging.Level;

import jakarta.persistence.PersistenceUnitTransactionType;

import org.hibernate.reactive.provider.impl.ReactiveIntegrator;
import org.jboss.logging.Logger;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.RecorderBeanInitializedBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
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
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.customized.FormatMapperKind;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider;
import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildItem;
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
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            List<ReactiveDataSourceBuildItem> reactiveDataSources,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
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

        Optional<ReactiveDataSourceBuildItem> defaultReactiveDataSource = reactiveDataSources.stream()
                .filter(i -> i.isDefault())
                .findFirst();

        boolean enableDefaultPersistenceUnit = (defaultReactiveDataSource.isPresent() &&
                hibernateOrmConfig.namedPersistenceUnits().isEmpty())
                || hibernateOrmConfig.defaultPersistenceUnit().isAnyPropertySet();

        if (enableDefaultPersistenceUnit) {
            producePersistenceUnitFromConfig(hibernateOrmConfig, DEFAULT_PERSISTENCE_UNIT_NAME,
                    hibernateOrmConfig.defaultPersistenceUnit(), index,
                    enableDefaultPersistenceUnit,
                    reactiveDataSources,
                    jdbcDataSources,
                    applicationArchivesBuildItem, jpaModel, launchMode, capabilities,
                    systemProperties, nativeImageResources,
                    hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    unremovableBeans, dbKindDialectBuildItems);
        }

        for (Map.Entry<String, HibernateOrmConfigPersistenceUnit> persistenceUnitEntry : hibernateOrmConfig
                .namedPersistenceUnits()
                .entrySet()) {
            String namedPersistenceUnitName = persistenceUnitEntry.getKey();

            HibernateOrmConfigPersistenceUnit persistenceUnitConfig = hibernateOrmConfig.namedPersistenceUnits()
                    .get(namedPersistenceUnitName);

            producePersistenceUnitFromConfig(hibernateOrmConfig, namedPersistenceUnitName, persistenceUnitConfig, index,
                    enableDefaultPersistenceUnit, reactiveDataSources, jdbcDataSources,
                    applicationArchivesBuildItem, jpaModel, launchMode, capabilities,
                    systemProperties, nativeImageResources,
                    hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    unremovableBeans, dbKindDialectBuildItems);
        }
    }

    private static void producePersistenceUnitFromConfig(HibernateOrmConfig hibernateOrmConfig, String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig, CombinedIndexBuildItem index,
            boolean enableDefaultPersistenceUnit,
            List<ReactiveDataSourceBuildItem> reactiveDataSources,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            JpaModelBuildItem jpaModel, LaunchModeBuildItem launchMode,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {
        boolean datasourceNamed = persistenceUnitConfig.datasource().isPresent();

        Optional<JdbcDataSourceBuildItem> jdbcDataSource = findDataSourceWithNameDefault(persistenceUnitName,
                persistenceUnitConfig,
                jdbcDataSources,
                JdbcDataSourceBuildItem::getName,
                JdbcDataSourceBuildItem::isDefault);

        Optional<ReactiveDataSourceBuildItem> reactiveDataSource = findDataSourceWithNameDefault(persistenceUnitName,
                persistenceUnitConfig,
                reactiveDataSources,
                ReactiveDataSourceBuildItem::getName,
                ReactiveDataSourceBuildItem::isDefault);

        if (jdbcDataSource.isPresent() && reactiveDataSource.isEmpty() && datasourceNamed) {
            LOG.debugf("The datasource '%s' is blocking, do not create this PU '%s' as reactive",
                    persistenceUnitConfig.datasource().get(), persistenceUnitName);
            return;
        }

        if (jdbcDataSource.isEmpty() && reactiveDataSource.isEmpty() && datasourceNamed) {
            String dataSourceName = persistenceUnitConfig.datasource().get();
            throw PersistenceUnitUtil.unableToFindDataSource(persistenceUnitName, dataSourceName,
                    DataSourceUtil.dataSourceNotConfigured(dataSourceName));
        }

        Optional<String> datasourceName = reactiveDataSource.map(ReactiveDataSourceBuildItem::getName);
        Optional<String> explicitDialect = persistenceUnitConfig.dialect().dialect();
        Optional<String> explicitDbMinVersion = reactiveDataSource.flatMap(ReactiveDataSourceBuildItem::getVersion);
        Optional<String> dbKindOptional = reactiveDataSource.map(ReactiveDataSourceBuildItem::getDbKind);
        Optional<String> dbVersion = reactiveDataSource.flatMap(ReactiveDataSourceBuildItem::getVersion);

        if (dbKindOptional.isEmpty()) {
            throw new ConfigurationException(
                    "The datasource must be configured for Hibernate Reactive. Refer to https://quarkus.io/guides/datasource for guidance.",
                    Set.of("quarkus.datasource.db-kind", "quarkus.datasource.username",
                            "quarkus.datasource.password"));
        }

        QuarkusPersistenceUnitDescriptor reactivePU = generateReactivePersistenceUnit(
                hibernateOrmConfig, persistenceUnitName, index, persistenceUnitConfig, jpaModel,
                dbKindOptional, explicitDialect, explicitDbMinVersion, applicationArchivesBuildItem,
                launchMode.getLaunchMode(),
                systemProperties, nativeImageResources, hotDeploymentWatchedFiles, dbKindDialectBuildItems,
                enableDefaultPersistenceUnit);

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

    private static <T> Optional<T> findDataSourceWithNameDefault(String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            List<T> datasSources,
            Function<T, String> nameExtractor, Function<T, Boolean> defaultExtractor) {
        if (persistenceUnitConfig.datasource().isPresent()) {
            String dataSourceName = persistenceUnitConfig.datasource().get();
            return datasSources.stream()
                    .filter(i -> dataSourceName.equals(nameExtractor.apply(i)))
                    .findFirst();
        } else if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            return datasSources.stream()
                    .filter(i -> defaultExtractor.apply(i))
                    .findFirst();
        } else {
            // if it's not the default persistence unit, we mandate an explicit datasource to prevent common errors
            return Optional.empty();
        }
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
            HibernateOrmConfig hibernateOrmConfig,
            String persistenceUnitName,
            CombinedIndexBuildItem index,
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
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems, boolean enableDefaultPersistenceUnit) {

        Map<String, Set<String>> modelClassesAndPackagesPerPersistencesUnits = HibernateOrmProcessor
                .getModelClassesAndPackagesPerPersistenceUnits(hibernateOrmConfig, jpaModel, index.getIndex(),
                        enableDefaultPersistenceUnit);

        Set<String> modelClassesAndPackages = modelClassesAndPackagesPerPersistencesUnits
                .getOrDefault(persistenceUnitName, Collections.emptySet());

        if (modelClassesAndPackages.isEmpty()) {
            LOG.warnf("Could not find any entities affected to the Hibernate Reactive persistence unit.");
        }

        QuarkusPersistenceUnitDescriptor descriptor = new QuarkusPersistenceUnitDescriptor(
                persistenceUnitName,
                PersistenceUnitTransactionType.RESOURCE_LOCAL,
                new ArrayList<>(modelClassesAndPackages),
                new Properties(),
                true);

        Set<String> storageEngineCollector = new HashSet<>();

        setDialectAndStorageEngine(
                persistenceUnitName,
                dbKindOptional,
                explicitDialect,
                explicitDbMinVersion,
                dbKindDialectBuildItems,
                persistenceUnitConfig.dialect().storageEngine(),
                systemProperties,
                descriptor.getProperties()::setProperty,
                storageEngineCollector);

        configureProperties(descriptor, persistenceUnitConfig, hibernateOrmConfig, true);
        configureSqlLoadScript(persistenceUnitName, persistenceUnitConfig, applicationArchivesBuildItem, launchMode,
                nativeImageResources, hotDeploymentWatchedFiles, descriptor);

        return descriptor;
    }
}
