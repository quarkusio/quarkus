package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Collection;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.internal.ProvidedService;

import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.proxies.ProxyDefinitions;

public final class RecordedState {

    private final Dialect dialect;
    private final String dataSource;
    private final PrevalidatedQuarkusMetadata metadata;
    private final BuildTimeSettings settings;
    private final Collection<Integrator> integrators;
    private final Collection<ProvidedService> providedServices;
    private final IntegrationSettings integrationSettings;
    private final ProxyDefinitions proxyClassDefinitions;
    private final MultiTenancyStrategy multiTenancyStrategy;
    private final boolean isReactive;
    private final boolean fromPersistenceXml;

    public RecordedState(Dialect dialect, PrevalidatedQuarkusMetadata metadata,
            BuildTimeSettings settings, Collection<Integrator> integrators,
            Collection<ProvidedService> providedServices, IntegrationSettings integrationSettings,
            ProxyDefinitions classDefinitions, String dataSource, MultiTenancyStrategy strategy, boolean isReactive,
            boolean fromPersistenceXml) {
        this.dialect = dialect;
        this.metadata = metadata;
        this.settings = settings;
        this.integrators = integrators;
        this.providedServices = providedServices;
        this.integrationSettings = integrationSettings;
        this.proxyClassDefinitions = classDefinitions;
        this.dataSource = dataSource;
        this.multiTenancyStrategy = strategy;
        this.isReactive = isReactive;
        this.fromPersistenceXml = fromPersistenceXml;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public PrevalidatedQuarkusMetadata getMetadata() {
        return metadata;
    }

    public BuildTimeSettings getBuildTimeSettings() {
        return settings;
    }

    public Collection<Integrator> getIntegrators() {
        return integrators;
    }

    public Collection<ProvidedService> getProvidedServices() {
        return providedServices;
    }

    public IntegrationSettings getIntegrationSettings() {
        return integrationSettings;
    }

    public ProxyDefinitions getProxyClassDefinitions() {
        return proxyClassDefinitions;
    }

    public String getDataSource() {
        return dataSource;
    }

    public MultiTenancyStrategy getMultiTenancyStrategy() {
        return multiTenancyStrategy;
    }

    public boolean isReactive() {
        return isReactive;
    }

    public boolean isFromPersistenceXml() {
        return fromPersistenceXml;
    }
}
