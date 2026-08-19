package io.quarkus.hibernate.reactive.deployment;

import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.configureProperties;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.configureSqlLoadScript;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.isHibernateValidatorPresent;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.setDialectAndStorageEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import jakarta.persistence.PersistenceUnitTransactionType;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DataSourceRequestBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.JpaModelPerPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaPersistenceUnitModel;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDefinitionBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.PersistenceUnitLookupBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.PersistenceUnitRequestBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.SqlLoadScriptDefaultBuildItem;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider;
import io.quarkus.hibernate.reactive.runtime.HibernateReactivePersistenceUnitProviderHelper;
import io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Handles the lifecycle of {@link ProgrammingParadigm#REACTIVE reactive} persistence units:
 * collecting requests, producing definitions, declaring the resulting datasource needs,
 * and building persistence unit descriptors from configuration.
 * <p>
 * A <b>request</b> ({@link PersistenceUnitRequestBuildItem}) declares that a persistence unit
 * is needed, carrying a name, a {@link ProgrammingParadigm}, and a {@link Reason} explaining why.
 * A <b>definition</b> ({@link PersistenceUnitDefinitionBuildItem}) is produced by checking
 * each request against the {@link PersistenceUnitLookupBuildItem lookup}: if the lookup deems
 * the persistence unit unavailable, a {@code ConfigurationException} is thrown with the full
 * reason chain; otherwise a definition is produced for downstream consumption.
 *
 * @see io.quarkus.hibernate.orm.deployment.PersistenceUnitLookupProcessor
 * @see io.quarkus.hibernate.orm.deployment.PersistenceUnitDefinitionBlockingProcessor
 */
@BuildSteps(onlyIf = HibernateReactiveEnabled.class)
class PersistenceUnitDefinitionReactiveProcessor {

    @BuildStep
    void collectImplicitReactivePersistenceUnitRequests(Capabilities capabilities, HibernateOrmConfig config,
            JpaModelPerPersistenceUnitBuildItem jpaModelPerPersistenceUnit,
            PersistenceUnitLookupBuildItem lookupBuildItem,
            BuildProducer<PersistenceUnitRequestBuildItem> puRequests) {
        HibernateProcessorUtil.collectPersistenceUnitRequestsFromConfiguration(ProgrammingParadigm.REACTIVE,
                capabilities, config,
                jpaModelPerPersistenceUnit, lookupBuildItem, puRequests);

        // We don't derive requests from injection points of persistence unit related beans,
        // because those could just be referencing custom beans,
        // as we suggest in https://quarkus.io/guides/hibernate-orm#persistence-unit-active
        // TODO https://github.com/quarkusio/quarkus/issues/55217
        //  Find a way to collect injection points for a given PU that have no matching user-defined producer
    }

    @BuildStep
    void definePersistenceUnits(
            HibernateOrmConfig hibernateOrmConfig,
            PersistenceUnitLookupBuildItem lookupBuildItem,
            List<PersistenceUnitRequestBuildItem> puRequests,
            BuildProducer<PersistenceUnitDefinitionBuildItem> persistenceUnitDefinitions) {
        HibernateProcessorUtil.definePersistenceUnits(ProgrammingParadigm.REACTIVE, hibernateOrmConfig, lookupBuildItem,
                puRequests, List.of(), List.of(), persistenceUnitDefinitions);
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
        Optional<String> dbVersion = reactiveDataSource.getDbVersion();
        Optional<String> dbKindOptional = Optional.of(reactiveDataSource.getDbKind());

        QuarkusPersistenceUnitDescriptorWithSupportedDBKind reactivePUWithDBKind = generateReactivePersistenceUnit(
                hibernateOrmConfig, persistenceUnitName, persistenceUnitConfig, model,
                dbKindOptional, explicitDialect, dbVersion, applicationArchivesBuildItem,
                launchMode.getLaunchMode(),
                additionalSqlLoadScriptDefaults,
                nativeImageResources, hotDeploymentWatchedFiles, dbKindDialectBuildItems);

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
                        reactiveDataSource.getDbVersion(),
                        reactiveDataSource.isDbVersionUserSpecified(),
                        persistenceUnitConfig.dialect().dialect(),
                        entityClassNames,
                        io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy.NONE,
                        hibernateOrmConfig.database().ormCompatibilityVersion(),
                        persistenceUnitConfig.unsupportedProperties()),
                null,
                model.xmlMappings(),
                false,
                isHibernateValidatorPresent(capabilities)));
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
            Optional<String> dbVersion,
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
                dbVersion,
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
