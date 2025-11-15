package io.quarkus.hibernate.orm.runtime.boot;

import static org.hibernate.cfg.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.COLLECTION_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME;
import static org.hibernate.cfg.AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;
import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;
import static org.hibernate.internal.CoreLogging.messageLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.boot.internal.StandardJpaScanEnvironmentImpl;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.jpa.internal.util.LogHelper;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.service.Service;
import org.hibernate.service.internal.AbstractServiceRegistryImpl;
import org.hibernate.service.internal.ProvidedService;
import org.infinispan.quarkus.hibernate.cache.QuarkusInfinispanRegionFactory;

import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.proxies.ProxyDefinitions;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;
import io.quarkus.hibernate.orm.runtime.recording.RecordableBootstrap;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;
import io.quarkus.hibernate.orm.runtime.service.QuarkusStaticInitDialectFactory;
import io.quarkus.hibernate.orm.runtime.tenant.HibernateMultiTenantConnectionProvider;

/**
 * Alternative to EntityManagerFactoryBuilderImpl so to have full control of how MetadataBuilderImplementor
 * is created, which configuration properties are supportable, custom overrides, etc...
 */
public class FastBootMetadataBuilder {

    /**
     * Old deprecated constants:
     */
    @Deprecated
    private static final String JACC_PREFIX = "hibernate.jacc";
    @Deprecated
    private static final String JACC_ENABLED = "hibernate.jacc.enabled";
    @Deprecated
    private static final String WRAP_RESULT_SETS = "hibernate.jdbc.wrap_result_sets";
    @Deprecated
    private static final String ALLOW_ENHANCEMENT_AS_PROXY = "hibernate.bytecode.allow_enhancement_as_proxy";

    private static final CoreMessageLogger LOG = messageLogger(FastBootMetadataBuilder.class);

    private final PersistenceUnitDescriptor persistenceUnit;
    private final BuildTimeSettings buildTimeSettings;
    private final StandardServiceRegistry standardServiceRegistry;
    private final ManagedResources managedResources;
    private final MetadataBuilderImplementor metamodelBuilder;
    private final Collection<Class<? extends Integrator>> additionalIntegrators;
    private final Collection<ProvidedService<?>> providedServices;
    private final PreGeneratedProxies preGeneratedProxies;
    private final MultiTenancyStrategy multiTenancyStrategy;
    private final boolean isReactive;
    private final boolean fromPersistenceXml;
    private final boolean isHibernateValidatorPresent;
    private final List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors;

    @SuppressWarnings("unchecked")
    public FastBootMetadataBuilder(final QuarkusPersistenceUnitDefinition puDefinition, Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators, PreGeneratedProxies preGeneratedProxies) {
        this.persistenceUnit = puDefinition.getPersistenceUnitDescriptor();
        this.isReactive = puDefinition.isReactive();
        this.fromPersistenceXml = puDefinition.isFromPersistenceXml();
        this.isHibernateValidatorPresent = puDefinition.isHibernateValidatorPresent();
        this.additionalIntegrators = additionalIntegrators;
        this.preGeneratedProxies = preGeneratedProxies;
        this.integrationStaticDescriptors = puDefinition.getIntegrationStaticDescriptors();

        // Copying semantics from: new EntityManagerFactoryBuilderImpl( unit,
        // integration, instance );
        // Except we remove support for several legacy features and XML binding

        LogHelper.logPersistenceUnitInformation(persistenceUnit);

        final RecordableBootstrap ssrBuilder = RecordableBootstrapFactory.createRecordableBootstrapBuilder(puDefinition);

        // Should be set before calling mergeSettings()
        this.multiTenancyStrategy = puDefinition.getConfig().getMultiTenancyStrategy();
        final MergedSettings mergedSettings = mergeSettings(puDefinition);
        this.buildTimeSettings = createBuildTimeSettings(puDefinition, mergedSettings.getConfigurationValues());

        // Build the "standard" service registry
        ssrBuilder.applySettings(buildTimeSettings.getAllSettings());

        this.standardServiceRegistry = ssrBuilder.build();

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
         * The following trick uses the aforementioned steps to set up the EnversService and then turns it into
         * a ProvidedService so that it is not necessary to repeat all these complex steps during the reactivation
         * of the destroyed service registry in PreconfiguredServiceRegistryBuilder.
         *
         */
        for (Class<? extends Service> postBuildProvidedService : ssrBuilder.getPostBuildProvidedServices()) {
            providedServices.add(new ProvidedService(postBuildProvidedService,
                    standardServiceRegistry.getService(postBuildProvidedService)));
        }

        final MetadataSources metadataSources = new MetadataSources(ssrBuilder.getBootstrapServiceRegistry());
        // No need to populate annotatedClassNames/annotatedPackages: they are populated through scanning
        // XML mappings, however, cannot be contributed through the scanner,
        // which only allows specifying mappings as files/resources,
        // and we really don't want any XML parsing here...
        for (RecordableXmlMapping mapping : puDefinition.getXmlMappings()) {
            metadataSources.addXmlBinding(mapping.toHibernateOrmBinding());
        }

        this.metamodelBuilder = (MetadataBuilderImplementor) metadataSources
                .getMetadataBuilder(standardServiceRegistry);
        if (scanner != null) {
            this.metamodelBuilder.applyScanner(scanner);
        }
        populate(metamodelBuilder, mergedSettings.cacheRegionDefinitions);

        this.managedResources = MetadataBuildingProcess.prepare(metadataSources,
                metamodelBuilder.getBootstrapContext());

        applyMetadataBuilderContributor();

        // Unable to automatically handle:
        // AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING,
        // AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION,
        // AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT

        // for the time being we want to revoke access to the temp ClassLoader if one
        // was passed
        metamodelBuilder.applyTempClassLoader(null);

    }

    private BuildTimeSettings createBuildTimeSettings(QuarkusPersistenceUnitDefinition puDefinition,
            Map<String, Object> quarkusConfigSettings) {
        Map<String, String> quarkusConfigUnsupportedProperties = puDefinition.getConfig()
                .getQuarkusConfigUnsupportedProperties();
        Map<String, Object> allSettings = new HashMap<>(quarkusConfigSettings);

        // We'll log warnings about unsupported properties and overrides on startup.
        // (see io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider.buildRuntimeSettings)
        allSettings.putAll(quarkusConfigUnsupportedProperties);

        var databaseOrmCompatibilityVersion = puDefinition.getConfig().getDatabaseOrmCompatibilityVersion();
        Map<String, String> appliedDatabaseOrmCompatibilitySettings = new HashMap<>();
        for (Map.Entry<String, String> entry : databaseOrmCompatibilityVersion.settings(puDefinition.getConfig().getDbKind())
                .entrySet()) {
            // Not using putIfAbsent() because that would be ambiguous in case of null values
            if (!allSettings.containsKey(entry.getKey())) {
                appliedDatabaseOrmCompatibilitySettings.put(entry.getKey(), entry.getValue());
            }
        }
        allSettings.putAll(appliedDatabaseOrmCompatibilitySettings);

        // We keep a separate copy of settings coming from Quarkus config,
        // so that we can more easily differentiate between
        // properties coming from Quarkus and "unsupported" properties
        // on startup (see io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider.buildRuntimeSettings)
        return new BuildTimeSettings(puDefinition.getConfig(), quarkusConfigSettings,
                appliedDatabaseOrmCompatibilitySettings, allSettings);
    }

    /**
     * Simplified copy of
     * org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl#mergeSettings(org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor,
     * java.util.Map, org.hibernate.boot.registry.StandardServiceRegistryBuilder)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private MergedSettings mergeSettings(QuarkusPersistenceUnitDefinition puDefinition) {
        PersistenceUnitDescriptor persistenceUnit = puDefinition.getPersistenceUnitDescriptor();
        final MergedSettings mergedSettings = new MergedSettings();
        final Map cfg = mergedSettings.configurationValues;

        // first, apply persistence.xml-defined settings
        if (persistenceUnit.getProperties() != null) {
            cfg.putAll(persistenceUnit.getProperties());
        }

        cfg.put(PERSISTENCE_UNIT_NAME, persistenceUnit.getName());

        if (multiTenancyStrategy != null && multiTenancyStrategy != MultiTenancyStrategy.NONE
                && multiTenancyStrategy != MultiTenancyStrategy.DISCRIMINATOR) {
            // Note: the counterpart of this code, but for single-tenancy (injecting the datasource),
            // can be found in io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider.injectDataSource

            // We need to initialize the multi tenant connection provider
            // on static init as it is used in MetadataBuildingOptionsImpl
            // to determine if multi-tenancy is enabled.
            // Adding the service on runtime init would lead to unpredictable behavior
            // (metadata generated for a single-tenant application but runtime using multi-tenancy...).
            // Nothing is expected to actually retrieve a connection from the provider until runtime init, though.
            cfg.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
                    new HibernateMultiTenantConnectionProvider(puDefinition.getName()));
        }

        applyTransactionProperties(persistenceUnit, cfg);
        applyJdbcConnectionProperties(cfg);

        // unsupported FLUSH_BEFORE_COMPLETION

        if (readBooleanConfigurationValue(cfg, AvailableSettings.FLUSH_BEFORE_COMPLETION)) {
            cfg.put(AvailableSettings.FLUSH_BEFORE_COMPLETION, "false");
            LOG.definingFlushBeforeCompletionIgnoredInHem(AvailableSettings.FLUSH_BEFORE_COMPLETION);
        }

        // Quarkus specific

        cfg.put(AvailableSettings.ALLOW_METADATA_ON_BOOT, "false");

        // Disallow CDI during metadata building in anticipation for https://github.com/quarkusio/quarkus/issues/40897
        cfg.put(AvailableSettings.ALLOW_EXTENSIONS_IN_CDI, "false");

        //This shouldn't be encouraged, but sometimes it's really useful - and it used to be the default
        //in Hibernate ORM before the JPA spec would require to change this.
        //At this time of transitioning we'll only expose it as a global system property, so to allow usage
        //for special circumstances and yet not encourage this.
        //Also, definitely don't override anything which was explicitly set in the configuration.
        if (!cfg.containsKey(AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION)) {
            cfg.put(AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION,
                    Boolean.getBoolean(AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION));
        }

        if (cfg.containsKey(ALLOW_ENHANCEMENT_AS_PROXY)) {
            LOG.warn("Setting '" + ALLOW_ENHANCEMENT_AS_PROXY
                    + "' is being ignored: this property is no longer meaningful since Hibernate ORM 6");
            cfg.remove(ALLOW_ENHANCEMENT_AS_PROXY);
        }
        //Always Order batch updates as it prevents contention on the data (unless it was disabled)
        if (!cfg.containsKey(AvailableSettings.ORDER_UPDATES)) {
            cfg.put(AvailableSettings.ORDER_UPDATES, Boolean.TRUE.toString());
        }
        //Agroal already does disable auto-commit, so Hibernate ORM should trust that:
        cfg.put(AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, Boolean.TRUE.toString());

        /*
         * Set CONNECTION_HANDLING to DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION
         * as it generally performs better, at no known drawbacks.
         * This is a new mode in Hibernate ORM, it might become the default in the future.
         *
         * Note: other connection handling modes lead to leaked resources, statements in particular.
         * See https://github.com/quarkusio/quarkus/issues/7242, https://github.com/quarkusio/quarkus/issues/13273
         *
         * @see org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode
         */
        cfg.putIfAbsent(AvailableSettings.CONNECTION_HANDLING,
                PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION);
        if (cfg.containsKey(WRAP_RESULT_SETS)) {
            LOG.warn("Wrapping result sets is no longer supported by Hibernate ORM. Setting " + WRAP_RESULT_SETS

                    + " is being ignored.");
            cfg.remove(WRAP_RESULT_SETS);
        }

        // XML mapping support can be costly, so we only enable it when XML mappings are actually used
        // or when integrations (e.g. Envers) need it.
        List<String> integrationsRequiringXmlMapping = integrationStaticDescriptors.stream()
                .filter(HibernateOrmIntegrationStaticDescriptor::isXmlMappingRequired)
                .map(HibernateOrmIntegrationStaticDescriptor::getIntegrationName).collect(Collectors.toList());
        Optional<Boolean> xmlMappingEnabledOptional = readOptionalBooleanConfigurationValue(cfg, XML_MAPPING_ENABLED);
        if (!puDefinition.getXmlMappings().isEmpty() || !integrationsRequiringXmlMapping.isEmpty()) {
            if (xmlMappingEnabledOptional.isPresent() && !xmlMappingEnabledOptional.get()) {
                // Explicitly disabled even though we need it...
                LOG.warnf("XML mapping is necessary in persistence unit '%s':"
                        + " %d XML mapping files are used, and %d extensions require XML mapping (%s)."
                        + " Setting '%s' to false.",
                        persistenceUnit.getName(), XML_MAPPING_ENABLED,
                        puDefinition.getXmlMappings().size(), integrationsRequiringXmlMapping.size(),
                        integrationsRequiringXmlMapping);
            }
            cfg.put(XML_MAPPING_ENABLED, "true");
        } else {
            if (xmlMappingEnabledOptional.isPresent() && xmlMappingEnabledOptional.get()) {
                // Explicitly enabled even though we do not need it...
                LOG.warnf("XML mapping is not necessary in persistence unit '%s'."
                        + " Setting '%s' to false.",
                        persistenceUnit.getName(), XML_MAPPING_ENABLED);
            }
            cfg.put(XML_MAPPING_ENABLED, "false");
        }

        // Note: this one is not a boolean, just having the property enables it
        if (cfg.containsKey(JACC_ENABLED)) {
            LOG.warn("JACC integration is no longer supported by Hibernate ORM. Option ignored.");
            cfg.remove(JACC_ENABLED);
        }

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
                            "Found JACC permission grant [%s] in properties, but JACC is no longer supported by Hibernate ORM: ignoring!");
                } else if (keyString.startsWith(CLASS_CACHE_PREFIX)) {
                    mergedSettings.addCacheRegionDefinition(
                            parseCacheRegionDefinitionEntry(keyString.substring(CLASS_CACHE_PREFIX.length() + 1),
                                    valueString, CacheRegionDefinition.CacheRegionType.ENTITY));
                } else if (keyString.startsWith(COLLECTION_CACHE_PREFIX)) {
                    mergedSettings.addCacheRegionDefinition(
                            parseCacheRegionDefinitionEntry(keyString.substring(COLLECTION_CACHE_PREFIX.length() + 1),
                                    (String) entry.getValue(), CacheRegionDefinition.CacheRegionType.COLLECTION));
                } else if (keyString.startsWith("hibernate.ejb.classcache")) {
                    LOG.warn(
                            "Deprecated configuration property prefixed by 'hibernate.ejb.classcache' is being ignored. Suggestion: change prefix to 'hibernate.classcache'");
                } else if (keyString.startsWith("hibernate.ejb.collectioncache")) {
                    LOG.warn(
                            "Deprecated configuration property prefixed by 'hibernate.ejb.collectioncache' is being ignored. Suggestion: change prefix to 'hibernate.collectioncache'");
                }
            }
        }

        cfg.put(org.hibernate.cfg.AvailableSettings.CACHE_REGION_FACTORY,
                QuarkusInfinispanRegionFactory.class.getName());

        for (HibernateOrmIntegrationStaticDescriptor descriptor : integrationStaticDescriptors) {
            Optional<HibernateOrmIntegrationStaticInitListener> listenerOptional = descriptor.getInitListener();
            if (listenerOptional.isPresent()) {
                listenerOptional.get().contributeBootProperties(cfg::put);
            }
        }

        // If there's any mapping lib that we can work with available we'll set the default mapper:
        if (puDefinition.getJsonMapperCreator().isPresent()) {
            cfg.put(AvailableSettings.JSON_FORMAT_MAPPER, puDefinition.getJsonMapperCreator().get().create());
        }
        // If there's any mapping lib that we can work with available we'll set the default mapper:
        if (puDefinition.getXmlMapperCreator().isPresent()) {
            cfg.put(AvailableSettings.XML_FORMAT_MAPPER, puDefinition.getXmlMapperCreator().get().create());
        }

        return mergedSettings;
    }

    public RecordedState build() {
        MetadataImpl fullMeta = (MetadataImpl) MetadataBuildingProcess.complete(
                managedResources,
                metamodelBuilder.getBootstrapContext(),
                metamodelBuilder.getMetadataBuildingOptions() //INTERCEPT & DESTROY :)
        );

        IntegrationSettings.Builder integrationSettingsBuilder = new IntegrationSettings.Builder();

        for (HibernateOrmIntegrationStaticDescriptor descriptor : integrationStaticDescriptors) {
            Optional<HibernateOrmIntegrationStaticInitListener> listenerOptional = descriptor.getInitListener();
            if (listenerOptional.isPresent()) {
                listenerOptional.get().onMetadataInitialized(fullMeta, metamodelBuilder.getBootstrapContext(),
                        integrationSettingsBuilder::put);
            }
        }

        Dialect dialect = extractDialect();
        PrevalidatedQuarkusMetadata storeableMetadata = trimBootstrapMetadata(fullMeta);
        //Make sure that the service is destroyed after the metadata has been validated and trimmed, as validation needs to use it.
        destroyServiceRegistry();
        ProxyDefinitions proxyClassDefinitions = ProxyDefinitions.createFromMetadata(storeableMetadata, preGeneratedProxies);
        return new RecordedState(dialect, storeableMetadata, buildTimeSettings, getIntegrators(),
                providedServices, integrationSettingsBuilder.build(), proxyClassDefinitions, multiTenancyStrategy,
                isReactive, fromPersistenceXml);
    }

    private void destroyServiceRegistry() {
        final AbstractServiceRegistryImpl serviceRegistry = (AbstractServiceRegistryImpl) metamodelBuilder.getBootstrapContext()
                .getServiceRegistry();
        serviceRegistry.close();
        serviceRegistry.resetParent(null);
    }

    private PrevalidatedQuarkusMetadata trimBootstrapMetadata(MetadataImpl fullMeta) {
        MetadataImpl replacement = new MetadataImpl(
                fullMeta.getUUID(),
                fullMeta.getMetadataBuildingOptions(), //TODO Replace this
                fullMeta.getEntityBindingMap(),
                fullMeta.getComposites(),
                fullMeta.getGenericComponentsMap(),
                fullMeta.getEmbeddableDiscriminatorTypesMap(),
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

        return PrevalidatedQuarkusMetadata.validateAndWrap(replacement);
    }

    private Dialect extractDialect() {
        DialectFactory service = standardServiceRegistry.getService(DialectFactory.class);
        QuarkusStaticInitDialectFactory casted = (QuarkusStaticInitDialectFactory) service;
        return casted.getDialect();
    }

    private Collection<Integrator> getIntegrators() {
        LinkedHashSet<Integrator> integrators = new LinkedHashSet<>();
        if (isHibernateValidatorPresent) {
            integrators.add(new BeanValidationIntegrator());
        }
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

    @SuppressWarnings("rawtypes")
    private Optional<Boolean> readOptionalBooleanConfigurationValue(Map configurationValues, String propertyName) {
        Object propertyValue = configurationValues.get(propertyName);
        return propertyValue == null ? Optional.empty() : Optional.of(Boolean.parseBoolean(propertyValue.toString()));
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
            transactionType = persistenceUnit.getPersistenceUnitTransactionType();
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

    /**
     * Greatly simplified copy of
     * org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl#populate(org.hibernate.boot.MetadataBuilder,
     * org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.MergedSettings,
     * org.hibernate.boot.registry.StandardServiceRegistry, java.util.List)
     */
    protected void populate(MetadataBuilder metamodelBuilder, List<CacheRegionDefinition> cacheRegionDefinitions) {

        ((MetadataBuilderImplementor) metamodelBuilder).getBootstrapContext().markAsJpaBootstrap();

        metamodelBuilder.applyScanEnvironment(new StandardJpaScanEnvironmentImpl(persistenceUnit));
        metamodelBuilder.applyScanOptions(new StandardScanOptions(
                (String) buildTimeSettings.get(org.hibernate.cfg.AvailableSettings.SCANNER_DISCOVERY),
                persistenceUnit.isExcludeUnlistedClasses()));

        if (cacheRegionDefinitions != null) {
            cacheRegionDefinitions.forEach(metamodelBuilder::applyCacheRegionDefinition);
        }

        final TypeContributorList typeContributorList = (TypeContributorList) buildTimeSettings
                .get(JpaSettings.TYPE_CONTRIBUTORS);
        if (typeContributorList != null) {
            typeContributorList.getTypeContributors().forEach(metamodelBuilder::applyTypes);
        }
    }

    private void applyMetadataBuilderContributor() {
        Object metadataBuilderContributorSetting = buildTimeSettings
                .get(JpaSettings.METADATA_BUILDER_CONTRIBUTOR);

        if (metadataBuilderContributorSetting == null) {
            return;
        }

        MetadataBuilderContributor metadataBuilderContributor = loadSettingInstance(
                JpaSettings.METADATA_BUILDER_CONTRIBUTOR,
                metadataBuilderContributorSetting,
                MetadataBuilderContributor.class);

        if (metadataBuilderContributor != null) {
            metadataBuilderContributor.contribute(metamodelBuilder);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T loadSettingInstance(String settingName, Object settingValue, Class<T> clazz) {
        T instance = null;
        Class<? extends T> instanceClass = null;

        if (clazz.isAssignableFrom(settingValue.getClass())) {
            instance = (T) settingValue;
        } else if (settingValue instanceof Class) {
            instanceClass = (Class<? extends T>) settingValue;
        } else if (settingValue instanceof String) {
            String settingStringValue = (String) settingValue;
            if (standardServiceRegistry != null) {
                final ClassLoaderService classLoaderService = standardServiceRegistry.getService(ClassLoaderService.class);

                instanceClass = classLoaderService.classForName(settingStringValue);
            } else {
                try {
                    instanceClass = (Class<? extends T>) Class.forName(settingStringValue);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Can't load class: " + settingStringValue, e);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "The provided " + settingName + " setting value [" + settingValue + "] is not supported!");
        }

        if (instanceClass != null) {
            try {
                instance = instanceClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalArgumentException(
                        "The " + clazz.getSimpleName() + " class [" + instanceClass + "] could not be instantiated!",
                        e);
            }
        }

        return instance;
    }

}
