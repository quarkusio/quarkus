/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.hibernate.orm.runtime.boot;

import static org.hibernate.cfg.AvailableSettings.DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.JACC_ENABLED;
import static org.hibernate.cfg.AvailableSettings.JACC_PREFIX;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;
import static org.hibernate.cfg.AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.AvailableSettings.WRAP_RESULT_SETS;
import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;
import static org.hibernate.internal.HEMLogging.messageLogger;
import static org.hibernate.jpa.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.jpa.AvailableSettings.COLLECTION_CACHE_PREFIX;
import static org.hibernate.jpa.AvailableSettings.PERSISTENCE_UNIT_NAME;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.StandardJpaScanEnvironmentImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.jpa.internal.util.LogHelper;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.service.internal.AbstractServiceRegistryImpl;
import org.infinispan.quarkus.hibernate.cache.QuarkusInfinispanRegionFactory;

import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.recording.RecordableBootstrap;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;
import io.quarkus.hibernate.orm.runtime.recording.RecordingDialectFactory;
import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;

/**
 * Alternative to EntityManagerFactoryBuilderImpl so to have full control of how MetadataBuilderImplementor
 * is created, which configuration properties are supportable, custom overrides, etc...
 */
public class FastBootMetadataBuilder {

    private static final EntityManagerMessageLogger LOG = messageLogger(FastBootMetadataBuilder.class);

    private final PersistenceUnitDescriptor persistenceUnit;
    private final BuildTimeSettings buildTimeSettings;
    private final StandardServiceRegistry standardServiceRegistry;
    private final ManagedResources managedResources;
    private final MetadataBuilderImplementor metamodelBuilder;
    private final Object validatorFactory;

    public FastBootMetadataBuilder(final PersistenceUnitDescriptor persistenceUnit, Scanner scanner) {
        this.persistenceUnit = persistenceUnit;
        final ClassLoaderService providedClassLoaderService = FlatClassLoaderService.INSTANCE;

        // Copying semantics from: new EntityManagerFactoryBuilderImpl( unit,
        // integration, instance );
        // Except we remove support for several legacy features and XML binding
        final ClassLoader providedClassLoader = null;

        LogHelper.logPersistenceUnitInformation(persistenceUnit);

        // Build the boot-strap service registry, which mainly handles class loader
        // interactions
        final BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry(providedClassLoaderService);

        // merge configuration sources and build the "standard" service registry
        final RecordableBootstrap ssrBuilder = new RecordableBootstrap(bsr);

        insertStateRecorders(ssrBuilder);

        final MergedSettings mergedSettings = mergeSettings(persistenceUnit);
        this.buildTimeSettings = new BuildTimeSettings(mergedSettings.getConfigurationValues());

        // Build the "standard" service registry
        ssrBuilder.applySettings(buildTimeSettings.getSettings());
        this.standardServiceRegistry = ssrBuilder.build();
        registerIdentifierGenerators(standardServiceRegistry);

        final MetadataSources metadataSources = new MetadataSources(bsr);
        addPUManagedClassNamesToMetadataSources(persistenceUnit, metadataSources);

        this.metamodelBuilder = (MetadataBuilderImplementor) metadataSources
                .getMetadataBuilder(standardServiceRegistry);
        if (scanner != null) {
            this.metamodelBuilder.applyScanner(scanner);
        }
        populate(metamodelBuilder, mergedSettings.cacheRegionDefinitions, standardServiceRegistry);

        this.managedResources = MetadataBuildingProcess.prepare(metadataSources,
                metamodelBuilder.getBootstrapContext());

        applyMetadataBuilderContributor();

        // BVAL integration:
        this.validatorFactory = withValidatorFactory(
                buildTimeSettings.get(org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_FACTORY));

        // Unable to automatically handle:
        // AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING,
        // AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION,
        // AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT

        // for the time being we want to revoke access to the temp ClassLoader if one
        // was passed
        metamodelBuilder.applyTempClassLoader(null);
    }

    private void addPUManagedClassNamesToMetadataSources(PersistenceUnitDescriptor persistenceUnit,
            MetadataSources metadataSources) {
        for (String className : persistenceUnit.getManagedClassNames()) {
            metadataSources.addAnnotatedClassName(className);
        }
    }

    private void insertStateRecorders(StandardServiceRegistryBuilder ssrBuilder) {
        //        ssrBuilder.addService( DialectFactory.class, new RecordingDialectFactory() );
        //        ssrBuilder.addInitiator(  )
    }

    private BootstrapServiceRegistry buildBootstrapServiceRegistry(ClassLoaderService providedClassLoaderService) {

        final BootstrapServiceRegistryBuilder bsrBuilder = new BootstrapServiceRegistryBuilder();

        // N.B. support for custom IntegratorProvider injected via Properties (as
        // instance) removed

        // N.B. support for custom StrategySelector removed
        // TODO see to inject a custom
        // org.hibernate.boot.registry.selector.spi.StrategySelector ?

        bsrBuilder.applyClassLoaderService(providedClassLoaderService);

        return bsrBuilder.build();
    }

    /**
     * Simplified copy of
     * org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl#mergeSettings(org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor,
     * java.util.Map, org.hibernate.boot.registry.StandardServiceRegistryBuilder)
     */
    @SuppressWarnings("unchecked")
    private MergedSettings mergeSettings(PersistenceUnitDescriptor persistenceUnit) {
        final MergedSettings mergedSettings = new MergedSettings();

        // first, apply persistence.xml-defined settings
        if (persistenceUnit.getProperties() != null) {
            mergedSettings.configurationValues.putAll(persistenceUnit.getProperties());
        }

        if (persistenceUnit.getJtaDataSource() != null) {
            mergedSettings.configurationValues.put(DATASOURCE, persistenceUnit.getJtaDataSource());
        }

        mergedSettings.configurationValues.put(PERSISTENCE_UNIT_NAME, persistenceUnit.getName());

        applyTransactionProperties(persistenceUnit, mergedSettings.configurationValues);
        applyJdbcConnectionProperties(mergedSettings.configurationValues);

        // unsupported FLUSH_BEFORE_COMPLETION

        if (readBooleanConfigurationValue(mergedSettings.configurationValues, Environment.FLUSH_BEFORE_COMPLETION)) {
            mergedSettings.configurationValues.put(Environment.FLUSH_BEFORE_COMPLETION, "false");
            LOG.definingFlushBeforeCompletionIgnoredInHem(Environment.FLUSH_BEFORE_COMPLETION);
        }

        // Quarkus specific

        mergedSettings.configurationValues.put("hibernate.temp.use_jdbc_metadata_defaults", "false");

        if (readBooleanConfigurationValue(mergedSettings.configurationValues, WRAP_RESULT_SETS)) {
            LOG.warn("Wrapping result sets is not supported. Setting " + WRAP_RESULT_SETS + " to false.");
        }
        mergedSettings.configurationValues.put(WRAP_RESULT_SETS, "false");

        if (readBooleanConfigurationValue(mergedSettings.configurationValues, XML_MAPPING_ENABLED)) {
            LOG.warn("XML mapping is not supported. Setting " + XML_MAPPING_ENABLED + " to false.");
        }
        mergedSettings.configurationValues.put(XML_MAPPING_ENABLED, "false");

        // Note: this one is not a boolean, just having the property enables it
        if (mergedSettings.configurationValues.containsKey(JACC_ENABLED)) {
            LOG.warn("JACC is not supported. Disabling it.");
        }
        mergedSettings.configurationValues.remove(JACC_ENABLED);

        enableCachingByDefault(mergedSettings.configurationValues);

        // here we are going to iterate the merged config settings looking for:
        // 1) additional JACC permissions
        // 2) additional cache region declarations
        //
        // we will also clean up any references with null entries
        Iterator itr = mergedSettings.configurationValues.entrySet().iterator();
        while (itr.hasNext()) {
            final Map.Entry entry = (Map.Entry) itr.next();
            if (entry.getValue() == null) {
                // remove entries with null values
                itr.remove();
                break;
            }

            if (String.class.isInstance(entry.getKey()) && String.class.isInstance(entry.getValue())) {
                final String keyString = (String) entry.getKey();
                final String valueString = (String) entry.getValue();

                if (keyString.startsWith(JACC_PREFIX)) {
                    LOG.warn(
                            "Found JACC permission grant [%s] in properties, but JACC is not compatible with the FastBootMetadataBuilder; ignoring!");
                } else if (keyString.startsWith(CLASS_CACHE_PREFIX)) {
                    mergedSettings.addCacheRegionDefinition(
                            parseCacheRegionDefinitionEntry(keyString.substring(CLASS_CACHE_PREFIX.length() + 1),
                                    valueString, CacheRegionDefinition.CacheRegionType.ENTITY));
                } else if (keyString.startsWith(COLLECTION_CACHE_PREFIX)) {
                    mergedSettings.addCacheRegionDefinition(
                            parseCacheRegionDefinitionEntry(keyString.substring(COLLECTION_CACHE_PREFIX.length() + 1),
                                    (String) entry.getValue(), CacheRegionDefinition.CacheRegionType.COLLECTION));
                }
            }
        }

        mergedSettings.configurationValues.put(org.hibernate.cfg.AvailableSettings.CACHE_REGION_FACTORY,
                QuarkusInfinispanRegionFactory.class.getName());

        return mergedSettings;
    }

    /**
     * Enable 2LC for entities and queries by default. Also allow "reference caching" by default.
     */
    private void enableCachingByDefault(final Map configurationValues) {
        //Only set these if the user isn't making an explicit choice:
        configurationValues.putIfAbsent(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.TRUE);
        configurationValues.putIfAbsent(USE_SECOND_LEVEL_CACHE, Boolean.TRUE);
        configurationValues.putIfAbsent(USE_QUERY_CACHE, Boolean.TRUE);
        configurationValues.putIfAbsent(JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE);
    }

    public RecordedState build() {
        MetadataImpl fullMeta = (MetadataImpl) MetadataBuildingProcess.complete(
                managedResources,
                metamodelBuilder.getBootstrapContext(),
                metamodelBuilder.getMetadataBuildingOptions() //INTERCEPT & DESTROY :)
        );
        Dialect dialect = extractDialect();
        JtaPlatform jtaPlatform = extractJtaPlatform();
        destroyServiceRegistry(fullMeta);
        MetadataImplementor storeableMetadata = trimBootstrapMetadata(fullMeta);
        return new RecordedState(dialect, jtaPlatform, storeableMetadata, buildTimeSettings);
    }

    private void destroyServiceRegistry(MetadataImplementor fullMeta) {
        final AbstractServiceRegistryImpl serviceRegistry = (AbstractServiceRegistryImpl) metamodelBuilder.getBootstrapContext()
                .getServiceRegistry();
        serviceRegistry.close();
        serviceRegistry.resetParent(null);
    }

    private MetadataImplementor trimBootstrapMetadata(MetadataImpl fullMeta) {
        MetadataImpl replacement = new MetadataImpl(
                fullMeta.getUUID(),
                fullMeta.getMetadataBuildingOptions(), //TODO Replace this
                fullMeta.getIdentifierGeneratorFactory(),
                fullMeta.getEntityBindingMap(),
                fullMeta.getMappedSuperclassMap(),
                fullMeta.getCollectionBindingMap(),
                fullMeta.getTypeDefinitionMap(),
                fullMeta.getFilterDefinitions(),
                fullMeta.getFetchProfileMap(),
                fullMeta.getImports(), // ok
                fullMeta.getIdGeneratorDefinitionMap(),
                fullMeta.getNamedQueryMap(),
                fullMeta.getNamedNativeQueryMap(), // TODO // might contain references to org.hibernate.loader.custom.ConstructorResultColumnProcessor, org.hibernate.type.TypeStandardSQLFunction
                fullMeta.getNamedProcedureCallMap(),
                fullMeta.getSqlResultSetMappingMap(), //TODO might contain NativeSQLQueryReturn (as namedNativeQueryMap above)
                fullMeta.getNamedEntityGraphs(), //TODO //reference to *annotation* instance ! FIXME or ignore feature?
                fullMeta.getSqlFunctionMap(), //ok
                fullMeta.getDatabase(), //Cleaned up: used to include references to MetadataBuildingOptions, etc..
                fullMeta.getBootstrapContext() //FIXME WHOA!
        );

        return replacement;
    }

    private JtaPlatform extractJtaPlatform() {
        return new JBossStandAloneJtaPlatform();
        //        JtaPlatformResolver service = standardServiceRegistry.getService( JtaPlatformResolver.class );
        //        return service.resolveJtaPlatform( this.configurationValues, (ServiceRegistryImplementor) standardServiceRegistry );
    }

    private Dialect extractDialect() {
        DialectFactory service = standardServiceRegistry.getService(DialectFactory.class);
        RecordingDialectFactory casted = (RecordingDialectFactory) service;
        return casted.getDialect();
    }

    private static class MergedSettings {
        private final Map configurationValues = new ConcurrentHashMap(16, 0.75f, 1);
        private List<CacheRegionDefinition> cacheRegionDefinitions;

        private MergedSettings() {
        }

        public Map getConfigurationValues() {
            return configurationValues;
        }

        private void addCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
            if (this.cacheRegionDefinitions == null) {
                this.cacheRegionDefinitions = new ArrayList<>();
            }
            this.cacheRegionDefinitions.add(cacheRegionDefinition);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean readBooleanConfigurationValue(Map configurationValues, String propertyName) {
        Object propertyValue = configurationValues.get(propertyName);
        return propertyValue != null && Boolean.parseBoolean(propertyValue.toString());
    }

    private CacheRegionDefinition parseCacheRegionDefinitionEntry(String role, String value,
            CacheRegionDefinition.CacheRegionType cacheType) {
        final StringTokenizer params = new StringTokenizer(value, ";, ");
        if (!params.hasMoreTokens()) {
            StringBuilder error = new StringBuilder("Illegal usage of ");
            if (cacheType == CacheRegionDefinition.CacheRegionType.ENTITY) {
                error.append(CLASS_CACHE_PREFIX).append(": ").append(CLASS_CACHE_PREFIX);
            } else {
                error.append(COLLECTION_CACHE_PREFIX).append(": ").append(COLLECTION_CACHE_PREFIX);
            }
            error.append('.').append(role).append(' ').append(value)
                    .append(".  Was expecting configuration (usage[,region[,lazy]]), but found none");
            throw persistenceException(error.toString());
        }

        String usage = params.nextToken();
        String region = null;
        if (params.hasMoreTokens()) {
            region = params.nextToken();
        }
        boolean lazyProperty = true;
        if (cacheType == CacheRegionDefinition.CacheRegionType.ENTITY) {
            if (params.hasMoreTokens()) {
                lazyProperty = "all".equalsIgnoreCase(params.nextToken());
            }
        } else {
            lazyProperty = false;
        }

        return new CacheRegionDefinition(cacheType, role, usage, region, lazyProperty);
    }

    private PersistenceException persistenceException(String message) {
        return persistenceException(message, null);
    }

    private PersistenceException persistenceException(String message, Exception cause) {
        return new PersistenceException(getExceptionHeader() + message, cause);
    }

    private String getExceptionHeader() {
        return "[PersistenceUnit: " + persistenceUnit.getName() + "] ";
    }

    private static void applyJdbcConnectionProperties(Map<String, Object> configurationValues) {
        final String driver = (String) configurationValues.get(JPA_JDBC_DRIVER);
        if (StringHelper.isNotEmpty(driver)) {
            configurationValues.put(DRIVER, driver);
        }
        final String url = (String) configurationValues.get(JPA_JDBC_URL);
        if (StringHelper.isNotEmpty(url)) {
            configurationValues.put(URL, url);
        }
        final String user = (String) configurationValues.get(JPA_JDBC_USER);
        if (StringHelper.isNotEmpty(user)) {
            configurationValues.put(USER, user);
        }
        final String pass = (String) configurationValues.get(JPA_JDBC_PASSWORD);
        if (StringHelper.isNotEmpty(pass)) {
            configurationValues.put(PASS, pass);
        }
    }

    private static void applyTransactionProperties(PersistenceUnitDescriptor persistenceUnit,
            Map<String, Object> configurationValues) {
        PersistenceUnitTransactionType transactionType = PersistenceUnitTransactionTypeHelper
                .interpretTransactionType(configurationValues.get(JPA_TRANSACTION_TYPE));
        if (transactionType == null) {
            transactionType = persistenceUnit.getTransactionType();
        }
        if (transactionType == null) {
            // is it more appropriate to have this be based on bootstrap entry point (EE vs SE)?
            transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }
        boolean hasTransactionStrategy = configurationValues.containsKey(TRANSACTION_COORDINATOR_STRATEGY);
        if (hasTransactionStrategy) {
            LOG.overridingTransactionStrategyDangerous(TRANSACTION_COORDINATOR_STRATEGY);
        } else {
            if (transactionType == PersistenceUnitTransactionType.JTA) {
                configurationValues.put(TRANSACTION_COORDINATOR_STRATEGY,
                        JtaTransactionCoordinatorBuilderImpl.class);
            } else if (transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
                configurationValues.put(TRANSACTION_COORDINATOR_STRATEGY,
                        JdbcResourceLocalTransactionCoordinatorBuilderImpl.class);
            }
        }
    }

    private void registerIdentifierGenerators(StandardServiceRegistry ssr) {
        final StrategySelector strategySelector = ssr.getService(StrategySelector.class);

        // apply id generators
        final Object idGeneratorStrategyProviderSetting = buildTimeSettings
                .get(AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER);
        if (idGeneratorStrategyProviderSetting != null) {
            final IdentifierGeneratorStrategyProvider idGeneratorStrategyProvider = strategySelector
                    .resolveStrategy(IdentifierGeneratorStrategyProvider.class, idGeneratorStrategyProviderSetting);
            final MutableIdentifierGeneratorFactory identifierGeneratorFactory = ssr
                    .getService(MutableIdentifierGeneratorFactory.class);
            if (identifierGeneratorFactory == null) {
                throw persistenceException("Application requested custom identifier generator strategies, "
                        + "but the MutableIdentifierGeneratorFactory could not be found");
            }
            for (Map.Entry<String, Class<?>> entry : idGeneratorStrategyProvider.getStrategies().entrySet()) {
                identifierGeneratorFactory.register(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Greatly simplified copy of
     * org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl#populate(org.hibernate.boot.MetadataBuilder,
     * org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.MergedSettings,
     * org.hibernate.boot.registry.StandardServiceRegistry, java.util.List)
     */
    protected void populate(MetadataBuilder metamodelBuilder, List<CacheRegionDefinition> cacheRegionDefinitions,
            StandardServiceRegistry ssr) {

        ((MetadataBuilderImplementor) metamodelBuilder).getBootstrapContext().markAsJpaBootstrap();

        metamodelBuilder.applyScanEnvironment(new StandardJpaScanEnvironmentImpl(persistenceUnit));
        metamodelBuilder.applyScanOptions(new StandardScanOptions(
                (String) buildTimeSettings.get(org.hibernate.cfg.AvailableSettings.SCANNER_DISCOVERY),
                persistenceUnit.isExcludeUnlistedClasses()));

        if (cacheRegionDefinitions != null) {
            cacheRegionDefinitions.forEach(metamodelBuilder::applyCacheRegionDefinition);
        }

        final TypeContributorList typeContributorList = (TypeContributorList) buildTimeSettings
                .get(EntityManagerFactoryBuilderImpl.TYPE_CONTRIBUTORS);
        if (typeContributorList != null) {
            typeContributorList.getTypeContributors().forEach(metamodelBuilder::applyTypes);
        }
    }

    private void applyMetadataBuilderContributor() {

        Object metadataBuilderContributorSetting = buildTimeSettings
                .get(EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR);

        if (metadataBuilderContributorSetting == null) {
            return;
        }

        MetadataBuilderContributor metadataBuilderContributor = null;
        Class<? extends MetadataBuilderContributor> metadataBuilderContributorImplClass = null;

        if (metadataBuilderContributorSetting instanceof MetadataBuilderContributor) {
            metadataBuilderContributor = (MetadataBuilderContributor) metadataBuilderContributorSetting;
        } else if (metadataBuilderContributorSetting instanceof Class) {
            metadataBuilderContributorImplClass = (Class<? extends MetadataBuilderContributor>) metadataBuilderContributorSetting;
        } else if (metadataBuilderContributorSetting instanceof String) {
            final ClassLoaderService classLoaderService = standardServiceRegistry.getService(ClassLoaderService.class);

            metadataBuilderContributorImplClass = classLoaderService
                    .classForName((String) metadataBuilderContributorSetting);
        } else {
            throw new IllegalArgumentException(
                    "The provided " + EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR + " setting value ["
                            + metadataBuilderContributorSetting + "] is not supported!");
        }

        if (metadataBuilderContributorImplClass != null) {
            try {
                metadataBuilderContributor = metadataBuilderContributorImplClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("The MetadataBuilderContributor class ["
                        + metadataBuilderContributorImplClass + "] could not be instantiated!", e);
            }
        }

        if (metadataBuilderContributor != null) {
            metadataBuilderContributor.contribute(metamodelBuilder);
        }
    }

    public Object withValidatorFactory(Object validatorFactory) {
        if (validatorFactory != null) {
            BeanValidationIntegrator.validateFactory(validatorFactory);
        }
        return validatorFactory;
    }

}
