package org.hibernate.protean.recording.customservices;

import java.util.Map;

import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class CfgXmlAccessServiceInitiatorProtean implements StandardServiceInitiator<CfgXmlAccessService> {

	public static final CfgXmlAccessServiceInitiatorProtean INSTANCE = new CfgXmlAccessServiceInitiatorProtean();

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