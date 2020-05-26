package io.quarkus.hibernate.reactive.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.hibernate.reactive.boot.service.ReactiveGenerationTarget;
import org.hibernate.reactive.jpa.impl.DelegatorPersistenceUnitInfo;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitsHolder;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrations;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;
import io.quarkus.hibernate.reactive.runtime.boot.FastBootReactiveEntityManagerFactoryBuilder;
import io.quarkus.hibernate.reactive.runtime.boot.registry.PreconfiguredReactiveServiceRegistryBuilder;
import io.quarkus.hibernate.reactive.runtime.customized.QuarkusReactiveConnectionPoolInitiator;
import io.vertx.sqlclient.Pool;

/**
 * This can not inherit from ReactivePersistenceProvider because it references HibernatePersistenceProvider
 * and that would trigger the native-image tool to include all code which could be triggered from using
 * that: we need to be able to fully exclude HibernatePersistenceProvider from the native image.
 */
final class FastBootHibernateReactivePersistenceProvider implements PersistenceProvider {

    private static final Logger log = Logger.getLogger(FastBootHibernateReactivePersistenceProvider.class);

    private static final String IMPLEMENTATION_NAME = "org.hibernate.reactive.jpa.impl.ReactivePersistenceProvider";

    private final FastBootHibernatePersistenceProvider delegate = new FastBootHibernatePersistenceProvider();

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

            RecordedState recordedState = PersistenceUnitsHolder.getRecordedState(persistenceUnitName);

            final PrevalidatedQuarkusMetadata metadata = recordedState.getMetadata();
            final BuildTimeSettings buildTimeSettings = recordedState.getBuildTimeSettings();
            final IntegrationSettings integrationSettings = recordedState.getIntegrationSettings();
            RuntimeSettings.Builder runtimeSettingsBuilder = new RuntimeSettings.Builder(buildTimeSettings,
                    integrationSettings);

            HibernateOrmIntegrations.contributeRuntimeProperties((k, v) -> runtimeSettingsBuilder.put(k, v));

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
                    validatorFactory, cdiBeanManager, recordedState.getMultiTenancyStrategy());
        }

        log.debug("Found no matching persistence units");
        return null;
    }

    private StandardServiceRegistry rewireMetadataAndExtractServiceRegistry(RuntimeSettings runtimeSettings,
            RecordedState rs,
            String persistenceUnitName) {
        PreconfiguredReactiveServiceRegistryBuilder serviceRegistryBuilder = new PreconfiguredReactiveServiceRegistryBuilder(
                rs);

        registerVertxPool(persistenceUnitName, runtimeSettings, serviceRegistryBuilder);

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

    private void registerVertxPool(String persistenceUnitName,
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
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        final String persistenceProviderClassName = info.getPersistenceProviderClassName();
        if (persistenceProviderClassName == null || IMPLEMENTATION_NAME.equals(persistenceProviderClassName)) {
            Map<Object, Object> protectiveCopy = map != null ? new HashMap<Object, Object>(map) : new HashMap<Object, Object>();
            return delegate.createContainerEntityManagerFactory(
                    new DelegatorPersistenceUnitInfo(info),
                    protectiveCopy);
        }
        //not the right provider
        return null;
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return delegate.getProviderUtil();
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map map) {
        throw new IllegalStateException("Hibernate Reactive does not support schema generation");
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
        throw new IllegalStateException("Hibernate Reactive does not support schema generation");
    }

}
