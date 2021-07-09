package io.quarkus.hibernate.reactive.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.reactive.provider.service.ReactiveGenerationTarget;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitsHolder;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings.Builder;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;
import io.quarkus.hibernate.reactive.runtime.boot.FastBootReactiveEntityManagerFactoryBuilder;
import io.quarkus.hibernate.reactive.runtime.boot.registry.PreconfiguredReactiveServiceRegistryBuilder;
import io.quarkus.hibernate.reactive.runtime.customized.QuarkusReactiveConnectionPoolInitiator;
import io.quarkus.hibernate.reactive.runtime.customized.VertxInstanceInitiator;
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
        try {
            // These are pre-parsed during image generation:
            final List<PersistenceUnitDescriptor> units = PersistenceUnitsHolder.getPersistenceUnitDescriptors();

            for (PersistenceUnitDescriptor unit : units) {
                //if the provider is not set, don't use it as people might want to use Hibernate ORM
                if (IMPLEMENTATION_NAME.equalsIgnoreCase(unit.getProviderClassName()) ||
                        unit.getProviderClassName() == null) {
                    EntityManagerFactoryBuilder emfBuilder = getEntityManagerFactoryBuilderOrNull(emName, properties);
                    EntityManagerFactory emf = emfBuilder.build();
                    return emf;
                }
            }

            //not the right provider
            return null;
        } catch (PersistenceException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PersistenceException("Unable to build EntityManagerFactory", e);
        }
    }

    private EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName,
            Map properties) {
        log.tracef("Attempting to obtain correct EntityManagerFactoryBuilder for persistenceUnitName : %s",
                persistenceUnitName);

        verifyProperties(properties);

        // These are pre-parsed during image generation:
        final List<PersistenceUnitDescriptor> units = PersistenceUnitsHolder.getPersistenceUnitDescriptors();

        log.debugf("Located %s persistence units; checking each", units.size());

        if (persistenceUnitName == null && units.size() > 1) {
            // no persistence-unit name to look for was given and we found multiple
            // persistence-units
            throw new PersistenceException("No name provided and multiple persistence units found");
        }

        for (PersistenceUnitDescriptor persistenceUnit : units) {
            log.debugf(
                    "Checking persistence-unit [name=%s, explicit-provider=%s] against incoming persistence unit name [%s]",
                    persistenceUnit.getName(), persistenceUnit.getProviderClassName(), persistenceUnitName);

            final boolean matches = persistenceUnitName == null
                    || persistenceUnit.getName().equals(persistenceUnitName);
            if (!matches) {
                log.debugf("Excluding from consideration '%s' due to name mis-match", persistenceUnit.getName());
                continue;
            }

            // See if we (Hibernate) are the persistence provider
            if (!isProvider(persistenceUnit)) {
                log.debug("Excluding from consideration due to provider mis-match");
                continue;
            }

            RecordedState recordedState = PersistenceUnitsHolder.popRecordedState(persistenceUnitName);

            final PrevalidatedQuarkusMetadata metadata = recordedState.getMetadata();
            final BuildTimeSettings buildTimeSettings = recordedState.getBuildTimeSettings();
            final IntegrationSettings integrationSettings = recordedState.getIntegrationSettings();
            RuntimeSettings.Builder runtimeSettingsBuilder = new RuntimeSettings.Builder(buildTimeSettings,
                    integrationSettings);

            // Inject runtime configuration if the persistence unit was defined by Quarkus configuration
            if (!recordedState.isFromPersistenceXml()) {
                injectRuntimeConfiguration(persistenceUnitName, hibernateOrmRuntimeConfig, runtimeSettingsBuilder);
            }

            for (HibernateOrmIntegrationRuntimeDescriptor descriptor : integrationRuntimeDescriptors
                    .getOrDefault(persistenceUnitName, Collections.emptyList())) {
                Optional<HibernateOrmIntegrationRuntimeInitListener> listenerOptional = descriptor.getInitListener();
                if (listenerOptional.isPresent()) {
                    listenerOptional.get().contributeRuntimeProperties(runtimeSettingsBuilder::put);
                }
            }

            RuntimeSettings runtimeSettings = runtimeSettingsBuilder.build();

            StandardServiceRegistry standardServiceRegistry = rewireMetadataAndExtractServiceRegistry(
                    runtimeSettings, recordedState, persistenceUnitName);

            final Object cdiBeanManager = Arc.container().beanManager();
            final Object validatorFactory = Arc.container().instance("quarkus-hibernate-validator-factory").get();

            return new FastBootReactiveEntityManagerFactoryBuilder(
                    metadata /* Uses the StandardServiceRegistry references by this! */,
                    persistenceUnitName,
                    standardServiceRegistry /* Mostly ignored! (yet needs to match) */,
                    runtimeSettings,
                    validatorFactory, cdiBeanManager);
        }

        log.debug("Found no matching persistence units");
        return null;
    }

    private StandardServiceRegistry rewireMetadataAndExtractServiceRegistry(RuntimeSettings runtimeSettings,
            RecordedState rs,
            String persistenceUnitName) {
        PreconfiguredReactiveServiceRegistryBuilder serviceRegistryBuilder = new PreconfiguredReactiveServiceRegistryBuilder(
                rs);

        registerVertxAndPool(persistenceUnitName, runtimeSettings, serviceRegistryBuilder);

        runtimeSettings.getSettings().forEach((key, value) -> {
            serviceRegistryBuilder.applySetting(key, value);
        });

        for (ProvidedService<?> providedService : rs.getProvidedServices()) {
            serviceRegistryBuilder.addService(providedService);
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
            PreconfiguredReactiveServiceRegistryBuilder serviceRegistry) {
        if (runtimeSettings.isConfigured(AvailableSettings.URL)) {
            // the pool has been defined in the persistence unit, we can bail out
            return;
        }

        // for now we only support one pool but this will change
        InstanceHandle<Pool> poolHandle = Arc.container().instance(Pool.class);
        if (!poolHandle.isAvailable()) {
            throw new IllegalStateException("No pool has been defined for persistence unit " + persistenceUnitName);
        }

        serviceRegistry.addInitiator(new QuarkusReactiveConnectionPoolInitiator(poolHandle.get()));

        InstanceHandle<Vertx> vertxHandle = Arc.container().instance(Vertx.class);
        if (!vertxHandle.isAvailable()) {
            throw new IllegalStateException("No Vert.x instance has been registered in ArC ?");
        }
        serviceRegistry.addInitiator(new VertxInstanceInitiator(vertxHandle.get()));
    }

    private static void injectRuntimeConfiguration(String persistenceUnitName,
            HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig, Builder runtimeSettingsBuilder) {
        HibernateOrmRuntimeConfigPersistenceUnit persistenceUnitConfig = hibernateOrmRuntimeConfig.defaultPersistenceUnit;

        // Database
        runtimeSettingsBuilder.put(AvailableSettings.HBM2DDL_DATABASE_ACTION,
                persistenceUnitConfig.database.generation.generation);

        runtimeSettingsBuilder.put(AvailableSettings.HBM2DDL_CREATE_SCHEMAS,
                String.valueOf(persistenceUnitConfig.database.generation.createSchemas));

        if (persistenceUnitConfig.database.generation.haltOnError) {
            runtimeSettingsBuilder.put(AvailableSettings.HBM2DDL_HALT_ON_ERROR, "true");
        }

        // Logging
        if (persistenceUnitConfig.log.sql) {
            runtimeSettingsBuilder.put(AvailableSettings.SHOW_SQL, "true");

            if (persistenceUnitConfig.log.formatSql) {
                runtimeSettingsBuilder.put(AvailableSettings.FORMAT_SQL, "true");
            }
        }

        if (persistenceUnitConfig.log.jdbcWarnings.isPresent()) {
            runtimeSettingsBuilder.put(AvailableSettings.LOG_JDBC_WARNINGS,
                    persistenceUnitConfig.log.jdbcWarnings.get().toString());
        }
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
