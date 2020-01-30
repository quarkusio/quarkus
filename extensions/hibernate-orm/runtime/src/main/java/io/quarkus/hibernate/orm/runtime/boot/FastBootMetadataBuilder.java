package io.quarkus.hibernate.orm.runtime.boot;

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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.StandardJpaScanEnvironmentImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.jpa.internal.util.LogHelper;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.service.Service;
import org.hibernate.service.internal.AbstractServiceRegistryImpl;
import org.hibernate.service.internal.ProvidedService;
import org.infinispan.quarkus.hibernate.cache.QuarkusInfinispanRegionFactory;

import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJtaPlatform;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrations;
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
    private final Collection<Class<? extends Integrator>> additionalIntegrators;
    private final Collection<ProvidedService> providedServices;

    @SuppressWarnings("unchecked")
    public FastBootMetadataBuilder(final PersistenceUnitDescriptor persistenceUnit, Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators) {
        this.persistenceUnit = persistenceUnit;
        this.additionalIntegrators = additionalIntegrators;
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

        this.providedServices = ssrBuilder.getProvidedServices();

        /**
         * This is required to properly integrate Hibernate Envers.
         *
         * The EnversService requires multiple steps to be properly built, the most important ones are:
         *
         * 1. The EnversServiceContributor contributes the EnversServiceInitiator to the RecordableBootstrap.
         * 2. After RecordableBootstrap builds a StandardServiceRegistry, the first time the EnversService is
         * requested, it is created by the initiator and configured by the registry.
         * 3. The MetadataBuildingProcess completes by calling the AdditionalJaxbMappingProducer which
         * initializes the EnversService and produces some additional mapping documents.
         * 4. After that point the EnversService appears to be fully functional.
         *
         * The following trick uses the aforementioned steps to setup the EnversService and then turns it into
         * a ProvidedService so that it is not necessary to repeat all these complex steps during the reactivation
         * of the destroyed service registry in PreconfiguredServiceRegistryBuilder.
         *
         */
        for (Class<? extends Service> postBuildProvidedService : ssrBuilder.getPostBuildProvidedServices()) {
            providedServices.add(new ProvidedService(postBuildProvidedService,
                    standardServiceRegistry.getService(postBuildProvidedService)));
        }

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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private MergedSettings mergeSettings(PersistenceUnitDescriptor persistenceUnit) {
        final MergedSettings mergedSettings = new MergedSettings();
        final Map cfg = mergedSettings.configurationValues;

        // first, apply persistence.xml-defined settings
        if (persistenceUnit.getProperties() != null) {
            cfg.putAll(persistenceUnit.getProperties());
        }

        cfg.put(PERSISTENCE_UNIT_NAME, persistenceUnit.getName());

        applyTransactionProperties(persistenceUnit, cfg);
        applyJdbcConnectionProperties(cfg);

        // unsupported FLUSH_BEFORE_COMPLETION

        if (readBooleanConfigurationValue(cfg, Environment.FLUSH_BEFORE_COMPLETION)) {
            cfg.put(Environment.FLUSH_BEFORE_COMPLETION, "false");
            LOG.definingFlushBeforeCompletionIgnoredInHem(Environment.FLUSH_BEFORE_COMPLETION);
        }

        // Quarkus specific

        cfg.put("hibernate.temp.use_jdbc_metadata_defaults", "false");

        //This shouldn't be encouraged but sometimes it's really useful - and it used to be the default
        //in Hibernate ORM before the JPA spec would require to change this.
        //At this time of transitioning we'll only expose it as a global system property, so to allow usage
        //for special circumstances and yet not encourage this.
        //Also, definitely don't override anything which was explicitly set in the configuration.
        if (!cfg.containsKey(AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION)) {
            cfg.put(AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION,
                    Boolean.getBoolean(AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION));
        }

        //Enable the new Enhanced Proxies capability (unless it was specifically disabled):
        if (!cfg.containsKey(AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY)) {
            cfg.put(AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, Boolean.TRUE.toString());
        }
        //Always Order batch updates as it prevents contention on the data (unless it was disabled)
        if (!cfg.containsKey(AvailableSettings.ORDER_UPDATES)) {
            cfg.put(AvailableSettings.ORDER_UPDATES, Boolean.TRUE.toString());
        }
        //Agroal already does disable auto-commit, so Hibernate ORM should trust that:
        cfg.put(AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, Boolean.TRUE.toString());

        /**
         * Set CONNECTION_HANDLING to DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION
         * as it generally performs better, at no drawbacks.
         * The reason it's not the default in Hibernate ORM is that some containers suspect it to leak connections
         * when running in this mode, as they trace resource handling across different beans.
         * 
         * @see org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode
         */
        {
            final Object explicitSetting = cfg.get(AvailableSettings.CONNECTION_HANDLING);
            if (explicitSetting == null) {
                cfg.put(AvailableSettings.CONNECTION_HANDLING,
                        PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION);
            }
        }

        if (readBooleanConfigurationValue(cfg, WRAP_RESULT_SETS)) {
            LOG.warn("Wrapping result sets is not supported. Setting " + WRAP_RESULT_SETS + " to false.");
        }
        cfg.put(WRAP_RESULT_SETS, "false");

        if (readBooleanConfigurationValue(cfg, XML_MAPPING_ENABLED)) {
            LOG.warn("XML mapping is not supported. Setting " + XML_MAPPING_ENABLED + " to false.");
        }
        cfg.put(XML_MAPPING_ENABLED, "false");

        // Note: this one is not a boolean, just having the property enables it
        if (cfg.containsKey(JACC_ENABLED)) {
            LOG.warn("JACC is not supported. Disabling it.");
        }
        cfg.remove(JACC_ENABLED);

        enableCachingByDefault(cfg);

        // here we are going to iterate the merged config settings looking for:
        // 1) additional JACC permissions
        // 2) additional cache region declarations
        //
        // we will also clean up any references with null entries
        Iterator itr = cfg.entrySet().iterator();
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

        cfg.put(org.hibernate.cfg.AvailableSettings.CACHE_REGION_FACTORY,
                QuarkusInfinispanRegionFactory.class.getName());

        HibernateOrmIntegrations.contributeBootProperties((k, v) -> cfg.put(k, v));

        return mergedSettings;
    }

    /**
     * Enable 2LC for entities and queries by default. Also allow "reference caching" by default.
     */
    private void enableCachingByDefault(final Map<String, Object> configurationValues) {
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

        IntegrationSettings.Builder integrationSettingsBuilder = new IntegrationSettings.Builder();
        HibernateOrmIntegrations.onMetadataInitialized(fullMeta, metamodelBuilder.getBootstrapContext(),
                (k, v) -> integrationSettingsBuilder.put(k, v));

        Dialect dialect = extractDialect();
        JtaPlatform jtaPlatform = extractJtaPlatform();
        destroyServiceRegistry(fullMeta);
        MetadataImplementor storeableMetadata = trimBootstrapMetadata(fullMeta);
        return new RecordedState(dialect, jtaPlatform, storeableMetadata, buildTimeSettings, getIntegrators(),
                providedServices, integrationSettingsBuilder.build());
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
        return QuarkusJtaPlatform.INSTANCE;
    }

    private Dialect extractDialect() {
        DialectFactory service = standardServiceRegistry.getService(DialectFactory.class);
        RecordingDialectFactory casted = (RecordingDialectFactory) service;
        return casted.getDialect();
    }

    private Collection<Integrator> getIntegrators() {
        LinkedHashSet<Integrator> integrators = new LinkedHashSet<>();
        integrators.add(new BeanValidationIntegrator());
        integrators.add(new CollectionCacheInvalidator());

        for (Class<? extends Integrator> integratorClass : additionalIntegrators) {
            try {
                integrators.add(integratorClass.getConstructor().newInstance());
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to instantiate integrator " + integratorClass, e);
            }
        }

        return integrators;
    }

    @SuppressWarnings("rawtypes")
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

    @SuppressWarnings("unchecked")
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
