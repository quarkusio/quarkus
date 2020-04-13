package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Collection;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.internal.ProvidedService;

import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.proxies.ProxyDefinitions;

public final class RecordedState {

    private final Dialect dialect;
    private final PrevalidatedQuarkusMetadata metadata;
    private final JtaPlatform jtaPlatform;
    private final BuildTimeSettings settings;
    private final Collection<Integrator> integrators;
    private final Collection<ProvidedService> providedServices;
    private final IntegrationSettings integrationSettings;
    private final ProxyDefinitions proxyClassDefinitions;
    private final MultiTenancyStrategy multiTenancyStrategy;

    public RecordedState(Dialect dialect, JtaPlatform jtaPlatform, PrevalidatedQuarkusMetadata metadata,
            BuildTimeSettings settings, Collection<Integrator> integrators,
            Collection<ProvidedService> providedServices, IntegrationSettings integrationSettings,
            ProxyDefinitions classDefinitions, MultiTenancyStrategy strategy) {
        this.dialect = dialect;
        this.jtaPlatform = jtaPlatform;
        this.metadata = metadata;
        this.settings = settings;
        this.integrators = integrators;
        this.providedServices = providedServices;
        this.integrationSettings = integrationSettings;
        this.proxyClassDefinitions = classDefinitions;
        this.multiTenancyStrategy = strategy;
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

    public JtaPlatform getJtaPlatform() {
        return jtaPlatform;
    }

    public ProxyDefinitions getProxyClassDefinitions() {
        return proxyClassDefinitions;
    }

    public MultiTenancyStrategy getMultiTenancyStrategy() {
        return multiTenancyStrategy;
    }
}
