package io.quarkus.hibernate.reactive.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.reactive.provider.service.ReactiveGenerationTarget;
import org.hibernate.service.Service;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitsHolder;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings.Builder;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;
import io.quarkus.hibernate.reactive.runtime.boot.FastBootReactiveEntityManagerFactoryBuilder;
import io.quarkus.hibernate.reactive.runtime.boot.registry.PreconfiguredReactiveServiceRegistryBuilder;
import io.quarkus.hibernate.reactive.runtime.customized.QuarkusReactiveConnectionPoolInitiator;
import io.quarkus.hibernate.reactive.runtime.customized.VertxInstanceInitiator;
import io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

/**
 * This can not inherit from ReactivePersistenceProvider because it references HibernatePersistenceProvider
 * and that would trigger the native-image tool to include all code which could be triggered from using
 * that: we need to be able to fully exclude HibernatePersistenceProvider from the native image.
 */
public final class FastBootHibernateReactivePersistenceProvider implements PersistenceProvider {

    private static final Logger log = Logger.getLogger(FastBootHibernateReactivePersistenceProvider.class);

    public static final String IMPLEMENTATION_NAME = "org.hibernate.reactive.provider.ReactivePersistenceProvider";

    private final ProviderUtil providerUtil = new io.quarkus.hibernate.orm.runtime.ProviderUtil();

    private volatile FastBootHibernatePersistenceProvider delegate;

    private final HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig;
    private final Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> integrationRuntimeDescriptors;

    public FastBootHibernateReactivePersistenceProvider(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> integrationRuntimeDescriptors) {
        this.hibernateOrmRuntimeConfig = hibernateOrmRuntimeConfig;
        this.integrationRuntimeDescriptors = integrationRuntimeDescriptors;
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map properties) {
        if (properties == null)
            properties = new HashMap<Object, Object>();
        // These are pre-parsed during image generation:

        // We might have multiple persistence unit descriptors of different kinds when using reactive and orm together
        // In this case this provider should take care only of reactive units
        // Luckily we can return null in this API to use the next provider for the other one
        // See jakarta.persistence.Persistence.createEntityManagerFactory(java.lang.String, java.util.Map)
        final List<QuarkusPersistenceUnitDescriptor> units = PersistenceUnitsHolder
                .getPersistenceUnitDescriptors()
                .stream()
                .filter(u -> u.getName().equals(emName))
                .filter(u -> u.isReactive()) // we don't support orm units here
                .toList();

        for (QuarkusPersistenceUnitDescriptor unit : units) {
            if (IMPLEMENTATION_NAME.equalsIgnoreCase(unit.getProviderClassName()) ||
                    unit.getProviderClassName() == null) {
                EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull(emName, properties);
                if (builder == null) {
                    log.trace("Could not obtain matching EntityManagerFactoryBuilder, returning null");
                    return null;
                } else {
                    return builder.build();
                }
            }
        }

        //not the right provider
        return null;
    }

    private EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName,
            Map properties) {
        log.tracef("Attempting to obtain correct EntityManagerFactoryBuilder for persistenceUnitName : %s",
                persistenceUnitName);

        verifyProperties(properties);

        // These are pre-parsed during image generation:
        // Processing only the reactive descriptors
        final List<QuarkusPersistenceUnitDescriptor> units = PersistenceUnitsHolder.getPersistenceUnitDescriptors()
                .stream()
                .filter(d -> d.isReactive())
                .toList();

        log.debugf("Located %s persistence units; checking each", units.size());

        if (persistenceUnitName == null && units.size() > 1) {
            // no persistence-unit name to look for was given and we found multiple
            // persistence-units
            throw new PersistenceException("No name provided and multiple persistence units found");
        }

        for (QuarkusPersistenceUnitDescriptor persistenceUnit : units) {
            log.debugf(
                    "Checking persistence-unit [name=%s, explicit-provider=%s] against incoming persistence unit name [%s]",
                    persistenceUnit.getName(), persistenceUnit.getProviderClassName(), persistenceUnitName);

            final boolean matches = persistenceUnitName == null
                    || persistenceUnit.getName().equals(persistenceUnitName);
            if (!matches) {
                log.debugf("Excluding from consideration '%s' due to name mismatch", persistenceUnit.getName());
                continue;
            }

            // See if we (Hibernate) are the persistence provider
            if (!isProvider(persistenceUnit)) {
                log.debug("Excluding from consideration due to provider mismatch");
                continue;
            }

            RecordedState recordedState = PersistenceUnitsHolder.popRecordedState(persistenceUnitName);

            final PrevalidatedQuarkusMetadata metadata = recordedState.getMetadata();
            final BuildTimeSettings buildTimeSettings = recordedState.getBuildTimeSettings();
            final IntegrationSettings integrationSettings = recordedState.getIntegrationSettings();
            RuntimeSettings.Builder runtimeSettingsBuilder = new RuntimeSettings.Builder(buildTimeSettings,
                    integrationSettings);

            var puConfig = hibernateOrmRuntimeConfig.persistenceUnits().get(persistenceUnit.getConfigurationName());
            if (puConfig.active().isPresent() && !puConfig.active().get()) {
                throw new IllegalStateException(
                        "Attempting to boot a deactivated Hibernate Reactive persistence unit");
            }

            // Inject runtime configuration if the persistence unit was defined by Quarkus configuration
            if (!recordedState.isFromPersistenceXml()) {
                injectRuntimeConfiguration(puConfig, runtimeSettingsBuilder);
            }

            for (HibernateOrmIntegrationRuntimeDescriptor descriptor : integrationRuntimeDescriptors
                    .getOrDefault(persistenceUnitName, Collections.emptyList())) {
                Optional<HibernateOrmIntegrationRuntimeInitListener> listenerOptional = descriptor.getInitListener();
                if (listenerOptional.isPresent()) {
                    listenerOptional.get().contributeRuntimeProperties(runtimeSettingsBuilder::put);
                }
            }

            // Allow detection of driver/database capabilities on runtime init (was disabled during static init)
            runtimeSettingsBuilder.put(AvailableSettings.ALLOW_METADATA_ON_BOOT, "true");
            // Remove database version information, if any;
            // it was necessary during static init to force creation of a dialect,
            // but now the dialect is there, and we'll reuse it.
            // Keeping this information would prevent us from getting the actual information from the database on start.
            runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION, null);

            if (!puConfig.unsupportedProperties().isEmpty()) {
                log.warnf("Persistence-unit [%s] sets unsupported properties."
                        + " These properties may not work correctly, and even if they do,"
                        + " that may change when upgrading to a newer version of Quarkus (even just a micro/patch version)."
                        + " Consider using a supported configuration property before falling back to unsupported ones."
                        + " If there is no supported equivalent, make sure to file a feature request so that a supported configuration property can be added to Quarkus,"
                        + " and more importantly so that the configuration property is tested regularly."
                        + " Unsupported properties being set: %s",
                        persistenceUnitName,
                        puConfig.unsupportedProperties().keySet());
            }
            Set<String> overriddenProperties = new HashSet<>();
            for (Map.Entry<String, String> entry : puConfig.unsupportedProperties().entrySet()) {
                var key = entry.getKey();
                var value = runtimeSettingsBuilder.get(key);
                if (value != null && !(value instanceof String stringValue && stringValue.isBlank())) {
                    overriddenProperties.add(key);
                }
                runtimeSettingsBuilder.put(entry.getKey(), entry.getValue());
            }
            if (!overriddenProperties.isEmpty()) {
                log.warnf("Persistence-unit [%s] sets unsupported properties that override Quarkus' own settings."
                        + " These properties may break assumptions in Quarkus code and cause malfunctions."
                        + " If this override is absolutely necessary, make sure to file a feature request or bug report so that a solution can be implemented in Quarkus."
                        + " Unsupported properties that override Quarkus' own settings: %s",
                        persistenceUnitName,
                        overriddenProperties);
            }

            RuntimeSettings runtimeSettings = runtimeSettingsBuilder.build();

            StandardServiceRegistry standardServiceRegistry = rewireMetadataAndExtractServiceRegistry(
                    persistenceUnitName, persistenceUnit.getConfigurationName(),
                    recordedState, runtimeSettings, puConfig);

            final Object cdiBeanManager = Arc.container().beanManager();
            final Object validatorFactory = Arc.container().instance("quarkus-hibernate-validator-factory").get();

            return new FastBootReactiveEntityManagerFactoryBuilder(
                    persistenceUnit,
                    metadata /* Uses the StandardServiceRegistry references by this! */,
                    standardServiceRegistry /* Mostly ignored! (yet needs to match) */,
                    runtimeSettings,
                    validatorFactory, cdiBeanManager, recordedState.getMultiTenancyStrategy(),
                    PersistenceUnitsHolder.getPersistenceUnitDescriptors().size() == 1,
                    recordedState.getBuildTimeSettings().getSource().getBuiltinFormatMapperBehaviour());
        }

        log.debug("Found no matching persistence units");
        return null;
    }

    private StandardServiceRegistry rewireMetadataAndExtractServiceRegistry(String persistenceUnitName,
            String persistenceUnitConfigurationName,
            RecordedState recordedState,
            RuntimeSettings runtimeSettings, HibernateOrmRuntimeConfigPersistenceUnit puConfig) {
        PreconfiguredReactiveServiceRegistryBuilder serviceRegistryBuilder = new PreconfiguredReactiveServiceRegistryBuilder(
                persistenceUnitConfigurationName, recordedState, puConfig);

        Optional<String> dataSourceName = recordedState.getBuildTimeSettings().getSource().getDataSource();
        if (dataSourceName.isPresent()) {
            registerVertxAndPool(persistenceUnitName, runtimeSettings, serviceRegistryBuilder, dataSourceName.get());
        }

        runtimeSettings.getSettings().forEach((key, value) -> {
            serviceRegistryBuilder.applySetting(key, value);
        });

        Set<Class<?>> runtimeInitiatedServiceClasses = new HashSet<>();
        for (HibernateOrmIntegrationRuntimeDescriptor descriptor : integrationRuntimeDescriptors
                .getOrDefault(persistenceUnitName, Collections.emptyList())) {
            Optional<HibernateOrmIntegrationRuntimeInitListener> listenerOptional = descriptor.getInitListener();
            if (listenerOptional.isPresent()) {
                for (StandardServiceInitiator<?> serviceInitiator : listenerOptional.get().contributeServiceInitiators()) {
                    Class<? extends Service> serviceClass = serviceInitiator.getServiceInitiated();
                    runtimeInitiatedServiceClasses.add(serviceClass);
                    serviceRegistryBuilder.addInitiator(serviceInitiator);
                }
            }
        }

        for (ProvidedService<?> providedService : recordedState.getProvidedServices()) {
            if (!runtimeInitiatedServiceClasses.contains(providedService.getServiceRole())) {
                serviceRegistryBuilder.addService(providedService);
            }
        }

        StandardServiceRegistryImpl standardServiceRegistry = serviceRegistryBuilder.buildNewServiceRegistry();

        standardServiceRegistry.getService(SchemaManagementTool.class)
                .setCustomDatabaseGenerationTarget(new ReactiveGenerationTarget(standardServiceRegistry));

        return standardServiceRegistry;
    }

    @SuppressWarnings("rawtypes")
    private void verifyProperties(Map properties) {
        if (properties != null && properties.size() != 0) {
            throw new PersistenceException(
                    "The FastbootHibernateProvider PersistenceProvider can not support runtime provided properties. "
                            + "Make sure you set all properties you need in the configuration resources before building the application.");
        }
    }

    private boolean isProvider(PersistenceUnitDescriptor persistenceUnit) {
        Map<Object, Object> props = Collections.emptyMap();
        String requestedProviderName = FastBootHibernatePersistenceProvider.extractRequestedProviderName(persistenceUnit,
                props);
        if (requestedProviderName == null) {
            // We'll always assume we are the best possible provider match unless the user
            // explicitly asks for a different one.
            return true;
        }
        return FastBootHibernateReactivePersistenceProvider.class.getName().equals(requestedProviderName)
                || IMPLEMENTATION_NAME.equals(requestedProviderName)
                || FastBootHibernatePersistenceProvider.class.getName().equals(requestedProviderName)
                || "org.hibernate.jpa.HibernatePersistenceProvider".equals(requestedProviderName);
    }

    private void registerVertxAndPool(String persistenceUnitName,
            RuntimeSettings runtimeSettings,
            PreconfiguredReactiveServiceRegistryBuilder serviceRegistry, String datasourceName) {
        if (runtimeSettings.isConfigured(AvailableSettings.URL)) {
            // the pool has been defined in the persistence unit, we can bail out
            return;
        }

        Pool pool;
        try {
            InjectableInstance<Pool> poolHandle = ReactiveDataSourceUtil.dataSourceInstance(datasourceName);
            if (!poolHandle.isResolvable()) {
                throw new IllegalStateException("No pool has been defined for persistence unit " + persistenceUnitName);
            }
            // ClientProxy.unwrap is necessary to trigger exceptions on inactive datasources
            pool = ClientProxy.unwrap(poolHandle.get());
        } catch (RuntimeException e) {
            throw PersistenceUnitUtil.unableToFindDataSource(persistenceUnitName, datasourceName, e);
        }

        serviceRegistry.addInitiator(new QuarkusReactiveConnectionPoolInitiator(pool));

        InstanceHandle<Vertx> vertxHandle = Arc.container().instance(Vertx.class);
        if (!vertxHandle.isAvailable()) {
            throw new IllegalStateException("No Vert.x instance has been registered in ArC ?");
        }
        serviceRegistry.addInitiator(new VertxInstanceInitiator(vertxHandle.get()));
    }

    private static void injectRuntimeConfiguration(HibernateOrmRuntimeConfigPersistenceUnit persistenceUnitConfig,
            Builder runtimeSettingsBuilder) {
        // Database
        runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
                persistenceUnitConfig.database().generation().generation()
                        .orElse(persistenceUnitConfig.schemaManagement().strategy()));

        runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS,
                String.valueOf(persistenceUnitConfig.database().generation().createSchemas()
                        .orElse(persistenceUnitConfig.schemaManagement().createSchemas())));

        if (persistenceUnitConfig.database().generation().haltOnError()
                .orElse(persistenceUnitConfig.schemaManagement().haltOnError())) {
            runtimeSettingsBuilder.put(AvailableSettings.HBM2DDL_HALT_ON_ERROR, "true");
        }

        //Never append on existing scripts:
        runtimeSettingsBuilder.put(AvailableSettings.HBM2DDL_SCRIPTS_CREATE_APPEND, "false");

        runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION,
                persistenceUnitConfig.scripts().generation().generation());

        if (persistenceUnitConfig.scripts().generation().createTarget().isPresent()) {
            runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET,
                    persistenceUnitConfig.scripts().generation().createTarget().get());
        }

        if (persistenceUnitConfig.scripts().generation().dropTarget().isPresent()) {
            runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET,
                    persistenceUnitConfig.scripts().generation().dropTarget().get());
        }

        persistenceUnitConfig.database().defaultCatalog().ifPresent(
                catalog -> runtimeSettingsBuilder.put(AvailableSettings.DEFAULT_CATALOG, catalog));

        persistenceUnitConfig.database().defaultSchema().ifPresent(
                schema -> runtimeSettingsBuilder.put(AvailableSettings.DEFAULT_SCHEMA, schema));

        // Logging
        if (persistenceUnitConfig.log().sql()) {
            runtimeSettingsBuilder.put(AvailableSettings.SHOW_SQL, "true");

            if (persistenceUnitConfig.log().formatSql()) {
                runtimeSettingsBuilder.put(AvailableSettings.FORMAT_SQL, "true");
            }

            if (persistenceUnitConfig.log().highlightSql()) {
                runtimeSettingsBuilder.put(AvailableSettings.HIGHLIGHT_SQL, "true");
            }
        }

        if (persistenceUnitConfig.log().jdbcWarnings().isPresent()) {
            runtimeSettingsBuilder.put(AvailableSettings.LOG_JDBC_WARNINGS,
                    persistenceUnitConfig.log().jdbcWarnings().get().toString());
        }

        if (persistenceUnitConfig.log().queriesSlowerThanMs().isPresent()) {
            runtimeSettingsBuilder.put(AvailableSettings.LOG_SLOW_QUERY,
                    persistenceUnitConfig.log().queriesSlowerThanMs().get());
        }

        runtimeSettingsBuilder.put(HibernateHints.HINT_FLUSH_MODE,
                persistenceUnitConfig.flush().mode());
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return providerUtil;
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        //Not supported by Hibernate Reactive: this should always delegate to Hibernate ORM, which will do its own
        //persistence provider name checks and possibly reject if it's not a suitable.
        return getJdbcHibernatePersistenceProviderDelegate().createContainerEntityManagerFactory(info, map);
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration configuration) {
        //Not supported by Hibernate Reactive: this should always delegate to Hibernate ORM, which will do its own
        //checks and possibly reject if it's not a suitable.
        return getJdbcHibernatePersistenceProviderDelegate().createEntityManagerFactory(configuration);
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map map) {
        getJdbcHibernatePersistenceProviderDelegate().generateSchema(info, map);
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
        return getJdbcHibernatePersistenceProviderDelegate().generateSchema(persistenceUnitName, map);
    }

    private FastBootHibernatePersistenceProvider getJdbcHibernatePersistenceProviderDelegate() {
        FastBootHibernatePersistenceProvider localDelegate = this.delegate;
        if (localDelegate == null) {
            synchronized (this) {
                localDelegate = this.delegate;
                if (localDelegate == null) {
                    this.delegate = localDelegate = new FastBootHibernatePersistenceProvider(hibernateOrmRuntimeConfig,
                            integrationRuntimeDescriptors);
                }
            }
        }
        return localDelegate;
    }

}
