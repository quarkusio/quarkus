package io.quarkus.hibernate.orm.runtime.recording;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.cfgxml.internal.CfgXmlAccessServiceInitiator;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.event.internal.EntityCopyObserverFactoryInitiator;
import org.hibernate.hql.internal.QueryTranslatorFactoryInitiator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyResolverInitiator;
import org.hibernate.resource.beans.spi.ManagedBeanRegistryInitiator;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractorInitiator;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;

import io.quarkus.hibernate.orm.runtime.boot.QuarkusEnvironment;
import io.quarkus.hibernate.orm.runtime.customized.BootstrapOnlyProxyFactoryFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProviderInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJndiServiceInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJtaPlatformInitiator;
import io.quarkus.hibernate.orm.runtime.service.DialectFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.DisabledJMXInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusMutableIdentifierGeneratorFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRegionFactoryInitiator;

/**
 * Has to extend StandardServiceRegistryBuilder even if we don't want: needs to
 * be assignable to it.
 */
public final class RecordableBootstrap extends StandardServiceRegistryBuilder {

    private static final String DISABLED_FEATURE_MSG = "This feature was disabled in Quarkus - this method should not have invoked, please report";

    private final Map settings;
    private final List<StandardServiceInitiator> initiators = standardInitiatorList();
    private final List<ProvidedService> providedServices = new ArrayList<ProvidedService>();
    private final List<Class<? extends Service>> postBuildProvidedServices = new ArrayList<>();

    private boolean autoCloseRegistry = true;

    private final BootstrapServiceRegistry bootstrapServiceRegistry;
    private final LoadedConfig aggregatedCfgXml;

    public RecordableBootstrap(BootstrapServiceRegistry bootstrapServiceRegistry) {
        this(bootstrapServiceRegistry, initialProperties(), LoadedConfig.baseline());
    }

    private static Map initialProperties() {
        HashMap map = new HashMap();
        map.putAll(QuarkusEnvironment.getInitialProperties());
        return map;
    }

    private RecordableBootstrap(BootstrapServiceRegistry bootstrapServiceRegistry, Map properties,
            LoadedConfig loadedConfigBaseline) {
        super(bootstrapServiceRegistry, properties, loadedConfigBaseline, null);
        this.settings = properties;
        this.bootstrapServiceRegistry = bootstrapServiceRegistry;
        this.aggregatedCfgXml = loadedConfigBaseline;
    }

    /**
     * Intended for internal testing use only!!
     */
    @Override
    public LoadedConfig getAggregatedCfgXml() {
        return aggregatedCfgXml;
    }

    // WARNING: this is a customized list: we started from a copy of ORM's standard
    // list, then changes have evolved.
    private static List<StandardServiceInitiator> standardInitiatorList() {
        final ArrayList<StandardServiceInitiator> serviceInitiators = new ArrayList<StandardServiceInitiator>();

        //This one needs to be replaced after Metadata has been recorded:
        serviceInitiators.add(BootstrapOnlyProxyFactoryFactoryInitiator.INSTANCE);

        serviceInitiators.add(CfgXmlAccessServiceInitiator.INSTANCE);
        serviceInitiators.add(ConfigurationServiceInitiator.INSTANCE);
        serviceInitiators.add(PropertyAccessStrategyResolverInitiator.INSTANCE);

        serviceInitiators.add(ImportSqlCommandExtractorInitiator.INSTANCE);
        serviceInitiators.add(SchemaManagementToolInitiator.INSTANCE);

        serviceInitiators.add(JdbcEnvironmentInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusJndiServiceInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(DisabledJMXInitiator.INSTANCE);

        serviceInitiators.add(PersisterClassResolverInitiator.INSTANCE);
        serviceInitiators.add(PersisterFactoryInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(MultiTenantConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(DialectResolverInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(DialectFactoryInitiator.INSTANCE);
        serviceInitiators.add(BatchBuilderInitiator.INSTANCE);
        serviceInitiators.add(JdbcServicesInitiator.INSTANCE);
        serviceInitiators.add(RefCursorSupportInitiator.INSTANCE);

        serviceInitiators.add(QueryTranslatorFactoryInitiator.INSTANCE);

        // Custom one! Also, this one has state so can't use the singleton.
        serviceInitiators.add(new QuarkusMutableIdentifierGeneratorFactoryInitiator());// MutableIdentifierGeneratorFactoryInitiator.INSTANCE);

        serviceInitiators.add(QuarkusJtaPlatformInitiator.INSTANCE);

        serviceInitiators.add(SessionFactoryServiceRegistryFactoryInitiator.INSTANCE);

        serviceInitiators.add(QuarkusRegionFactoryInitiator.INSTANCE);

        serviceInitiators.add(TransactionCoordinatorBuilderInitiator.INSTANCE);

        serviceInitiators.add(ManagedBeanRegistryInitiator.INSTANCE);

        serviceInitiators.add(EntityCopyObserverFactoryInitiator.INSTANCE);

        serviceInitiators.trimToSize();

        return serviceInitiators;
    }

    @Override
    public BootstrapServiceRegistry getBootstrapServiceRegistry() {
        return bootstrapServiceRegistry;
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public StandardServiceRegistryBuilder loadProperties(String resourceName) {
        throw new UnsupportedOperationException(DISABLED_FEATURE_MSG);
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public StandardServiceRegistryBuilder loadProperties(File file) {
        throw new UnsupportedOperationException(DISABLED_FEATURE_MSG);
    }

    /**
     * Read setting information from an XML file using the standard resource
     * location.
     *
     * @return this, for method chaining
     *
     * @see #DEFAULT_CFG_RESOURCE_NAME
     * @see #configure(String)
     * @see #loadProperties(String)
     */
    @Override
    public StandardServiceRegistryBuilder configure() {
        return configure(DEFAULT_CFG_RESOURCE_NAME);
    }

    @Override
    public StandardServiceRegistryBuilder configure(String resourceName) {
        throw new UnsupportedOperationException(DISABLED_FEATURE_MSG);
    }

    @Override
    public StandardServiceRegistryBuilder configure(File configurationFile) {
        throw new UnsupportedOperationException(DISABLED_FEATURE_MSG);
    }

    @Override
    public StandardServiceRegistryBuilder configure(URL url) {
        throw new UnsupportedOperationException(DISABLED_FEATURE_MSG);
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public StandardServiceRegistryBuilder configure(LoadedConfig loadedConfig) {
        throw new UnsupportedOperationException(DISABLED_FEATURE_MSG);
    }

    /**
     * Apply a setting value.
     *
     * @param settingName The name of the setting
     * @param value The value to use.
     *
     * @return this, for method chaining
     */
    @Override
    @SuppressWarnings({ "unchecked", "UnusedDeclaration" })
    public StandardServiceRegistryBuilder applySetting(String settingName, Object value) {
        settings.put(settingName, value);
        return this;
    }

    /**
     * Apply a groups of setting values.
     *
     * @param settings The incoming settings to apply
     *
     * @return this, for method chaining
     */
    @Override
    @SuppressWarnings({ "unchecked", "UnusedDeclaration" })
    public StandardServiceRegistryBuilder applySettings(Map settings) {
        this.settings.putAll(settings);
        return this;
    }

    @Override
    public void clearSettings() {
        settings.clear();
    }

    /**
     * Adds a service initiator.
     *
     * @param initiator The initiator to be added
     *
     * @return this, for method chaining
     */
    @Override
    @SuppressWarnings({ "UnusedDeclaration" })
    public StandardServiceRegistryBuilder addInitiator(StandardServiceInitiator initiator) {
        initiators.add(initiator);
        postBuildProvidedServices.add(initiator.getServiceInitiated());
        return this;
    }

    /**
     * Adds a user-provided service.
     *
     * @param serviceRole The role of the service being added
     * @param service The service implementation
     *
     * @return this, for method chaining
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public StandardServiceRegistryBuilder addService(final Class serviceRole, final Service service) {
        providedServices.add(new ProvidedService(serviceRole, service));
        return this;
    }

    /**
     * By default, when a ServiceRegistry is no longer referenced by any other
     * registries as a parent it will be closed.
     * <p/>
     * Some applications that explicitly build "shared registries" may want to
     * circumvent that behavior.
     * <p/>
     * This method indicates that the registry being built should not be
     * automatically closed. The caller agrees to take responsibility to close it
     * themselves.
     *
     * @return this, for method chaining
     */
    @Override
    public StandardServiceRegistryBuilder disableAutoClose() {
        this.autoCloseRegistry = false;
        return this;
    }

    /**
     * See the discussion on {@link #disableAutoClose}. This method enables the
     * auto-closing.
     *
     * @return this, for method chaining
     */
    @Override
    public StandardServiceRegistryBuilder enableAutoClose() {
        this.autoCloseRegistry = true;
        return this;
    }

    /**
     * Build the StandardServiceRegistry.
     *
     * @return The StandardServiceRegistry.
     */
    @Override
    @SuppressWarnings("unchecked")
    public StandardServiceRegistry build() {
        applyServiceContributors();

        final Map settingsCopy = new HashMap();
        settingsCopy.putAll(settings);
        settingsCopy.put(org.hibernate.boot.cfgxml.spi.CfgXmlAccessService.LOADED_CONFIG_KEY, aggregatedCfgXml);

        return new StandardServiceRegistryImpl(autoCloseRegistry, bootstrapServiceRegistry, initiators,
                providedServices, settingsCopy);
    }

    private void applyServiceContributors() {
        final Iterable<ServiceContributor> serviceContributors = bootstrapServiceRegistry
                .getService(ClassLoaderService.class).loadJavaServices(ServiceContributor.class);

        for (ServiceContributor serviceContributor : serviceContributors) {
            serviceContributor.contribute(this);
        }
    }

    public List<ProvidedService> getProvidedServices() {
        return providedServices;
    }

    /**
     * Temporarily exposed since Configuration is still around and much code still
     * uses Configuration. This allows code to configure the builder and access that
     * to configure Configuration object (used from HEM atm).
     *
     * @return The settings map.
     *
     * @deprecated Temporarily exposed since Configuration is still around and much
     *             code still uses Configuration. This allows code to configure the
     *             builder and access that to configure Configuration object.
     */
    @Override
    @Deprecated
    public Map getSettings() {
        return settings;
    }

    /**
     * Destroy a service registry. Applications should only destroy registries they
     * have explicitly created.
     *
     * @param serviceRegistry The registry to be closed.
     */
    public static void destroy(ServiceRegistry serviceRegistry) {
        if (serviceRegistry == null) {
            return;
        }

        ((StandardServiceRegistryImpl) serviceRegistry).destroy();
    }

    /**
     * @return the list of services to get from the service registry and turn into provided services
     */
    public List<Class<? extends Service>> getPostBuildProvidedServices() {
        return postBuildProvidedServices;
    }

}
