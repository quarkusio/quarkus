package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class CfgXmlAccessServiceInitiatorQuarkus implements StandardServiceInitiator<CfgXmlAccessService> {

    public static final CfgXmlAccessServiceInitiatorQuarkus INSTANCE = new CfgXmlAccessServiceInitiatorQuarkus();

    @Override
    public CfgXmlAccessService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new CfgXmlAccessService() {
            @Override
            public LoadedConfig getAggregatedConfig() {
                return null;
            }
        };
    }

    @Override
    public Class<CfgXmlAccessService> getServiceInitiated() {
        return CfgXmlAccessService.class;
    }
}