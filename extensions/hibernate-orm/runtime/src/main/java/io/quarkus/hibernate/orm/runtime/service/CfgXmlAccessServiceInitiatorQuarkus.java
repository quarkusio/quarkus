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