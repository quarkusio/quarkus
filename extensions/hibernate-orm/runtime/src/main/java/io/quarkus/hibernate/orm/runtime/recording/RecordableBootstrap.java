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

package io.quarkus.hibernate.orm.runtime.recording;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.cfgxml.internal.CfgXmlAccessServiceInitiator;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jndi.internal.JndiServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformResolverInitiator;
import org.hibernate.event.internal.EntityCopyObserverFactoryInitiator;
import org.hibernate.hql.internal.QueryTranslatorFactoryInitiator;
import org.hibernate.id.factory.internal.MutableIdentifierGeneratorFactoryInitiator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.internal.util.config.ConfigurationHelper;
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
import io.quarkus.hibernate.orm.runtime.service.DialectFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.DisabledJMXInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRegionFactoryInitiator;

/**
 * Has to extend StandardServiceRegistryBuilder even if we don't want: needs to
 * be assignable to it.
 */
public final class RecordableBootstrap extends StandardServiceRegistryBuilder {

    private final Map settings;
    private final List<StandardServiceInitiator> initiators = standardInitiatorList();
    private final List<ProvidedService> providedServices = new ArrayList<ProvidedService>();

    private boolean autoCloseRegistry = true;

    private final BootstrapServiceRegistry bootstrapServiceRegistry;
    private final ConfigLoader configLoader;
    private final LoadedConfig aggregatedCfgXml;

    public RecordableBootstrap(BootstrapServiceRegistry bootstrapServiceRegistry) {
        this(bootstrapServiceRegistry, LoadedConfig.baseline());
    }

    public RecordableBootstrap(BootstrapServiceRegistry bootstrapServiceRegistry, LoadedConfig loadedConfigBaseline) {
        this.settings = QuarkusEnvironment.getInitialProperties();
        this.bootstrapServiceRegistry = bootstrapServiceRegistry;
        this.configLoader = new ConfigLoader(bootstrapServiceRegistry);
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

        serviceInitiators.add(CfgXmlAccessServiceInitiator.INSTANCE);
        serviceInitiators.add(ConfigurationServiceInitiator.INSTANCE);
        serviceInitiators.add(PropertyAccessStrategyResolverInitiator.INSTANCE);

        serviceInitiators.add(ImportSqlCommandExtractorInitiator.INSTANCE);
        serviceInitiators.add(SchemaManagementToolInitiator.INSTANCE);

        serviceInitiators.add(JdbcEnvironmentInitiator.INSTANCE);
        serviceInitiators.add(JndiServiceInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(DisabledJMXInitiator.INSTANCE);

        serviceInitiators.add(PersisterClassResolverInitiator.INSTANCE);
        serviceInitiators.add(PersisterFactoryInitiator.INSTANCE);

        serviceInitiators.add(ConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(MultiTenantConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(DialectResolverInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(DialectFactoryInitiator.INSTANCE);
        serviceInitiators.add(BatchBuilderInitiator.INSTANCE);
        serviceInitiators.add(JdbcServicesInitiator.INSTANCE);
        serviceInitiators.add(RefCursorSupportInitiator.INSTANCE);

        serviceInitiators.add(QueryTranslatorFactoryInitiator.INSTANCE);
        serviceInitiators.add(MutableIdentifierGeneratorFactoryInitiator.INSTANCE);

        serviceInitiators.add(JtaPlatformResolverInitiator.INSTANCE);
        serviceInitiators.add(JtaPlatformInitiator.INSTANCE);

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

    /**
     * Read settings from a {@link java.util.Properties} file by resource name.
     * <p>
     * Differs from {@link #configure()} and {@link #configure(String)} in that here
     * we expect to read a {@link java.util.Properties} file while for
     * {@link #configure} we read the XML variant.
     *
     * @param resourceName The name by which to perform a resource look up for the
     *        properties file.
     *
     * @return this, for method chaining
     *
     * @see #configure()
     * @see #configure(String)
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public StandardServiceRegistryBuilder loadProperties(String resourceName) {
        settings.putAll(configLoader.loadProperties(resourceName));
        return this;
    }

    /**
     * Read settings from a {@link java.util.Properties} file by File reference
     * <p>
     * Differs from {@link #configure()} and {@link #configure(String)} in that here
     * we expect to read a {@link java.util.Properties} file while for
     * {@link #configure} we read the XML variant.
     *
     * @param file The properties File reference
     *
     * @return this, for method chaining
     *
     * @see #configure()
     * @see #configure(String)
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public StandardServiceRegistryBuilder loadProperties(File file) {
        settings.putAll(configLoader.loadProperties(file));
        return this;
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

    /**
     * Read setting information from an XML file using the named resource location.
     *
     * @param resourceName The named resource
     *
     * @return this, for method chaining
     */
    @Override
    public StandardServiceRegistryBuilder configure(String resourceName) {
        return configure(configLoader.loadConfigXmlResource(resourceName));
    }

    @Override
    public StandardServiceRegistryBuilder configure(File configurationFile) {
        return configure(configLoader.loadConfigXmlFile(configurationFile));
    }

    @Override
    public StandardServiceRegistryBuilder configure(URL url) {
        return configure(configLoader.loadConfigXmlUrl(url));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public StandardServiceRegistryBuilder configure(LoadedConfig loadedConfig) {
        aggregatedCfgXml.merge(loadedConfig);
        settings.putAll(loadedConfig.getConfigurationValues());

        return this;
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
        applyServiceContributingIntegrators();
        applyServiceContributors();

        final Map settingsCopy = new HashMap();
        settingsCopy.putAll(settings);
        settingsCopy.put(org.hibernate.boot.cfgxml.spi.CfgXmlAccessService.LOADED_CONFIG_KEY, aggregatedCfgXml);
        ConfigurationHelper.resolvePlaceHolders(settingsCopy);

        return new StandardServiceRegistryImpl(autoCloseRegistry, bootstrapServiceRegistry, initiators,
                providedServices, settingsCopy);
    }

    @SuppressWarnings("deprecation")
    private void applyServiceContributingIntegrators() {
        for (Integrator integrator : bootstrapServiceRegistry.getService(IntegratorService.class).getIntegrators()) {
            if (org.hibernate.integrator.spi.ServiceContributingIntegrator.class.isInstance(integrator)) {
                org.hibernate.integrator.spi.ServiceContributingIntegrator.class.cast(integrator).prepareServices(this);
            }
        }
    }

    private void applyServiceContributors() {
        final Iterable<ServiceContributor> serviceContributors = bootstrapServiceRegistry
                .getService(ClassLoaderService.class).loadJavaServices(ServiceContributor.class);

        for (ServiceContributor serviceContributor : serviceContributors) {
            serviceContributor.contribute(this);
        }
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
}
