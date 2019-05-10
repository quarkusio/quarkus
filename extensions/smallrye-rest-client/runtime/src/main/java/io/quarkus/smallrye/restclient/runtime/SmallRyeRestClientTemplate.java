/*
 *  Copyright (c) 2019 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.quarkus.smallrye.restclient.runtime;

import java.util.Set;

import javax.ws.rs.RuntimeType;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.jboss.resteasy.core.providerfactory.ClientHelper;
import org.jboss.resteasy.core.providerfactory.NOOPServerHelper;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.quarkus.runtime.annotations.Template;

@Template
public class SmallRyeRestClientTemplate {
    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

    public void setSslEnabled(boolean sslEnabled) {
        RestClientBuilderImpl.SSL_ENABLED = sslEnabled;
    }

    public void initializeResteasyProviderFactory(boolean useBuiltIn, Set<String> providersToRegister) {
        ResteasyProviderFactory clientProviderFactory = new ResteasyProviderFactoryImpl(null, true) {
            @Override
            public RuntimeType getRuntimeType() {
                return RuntimeType.CLIENT;
            }

            @Override
            protected void initializeUtils() {
                clientHelper = new ClientHelper(this);
                serverHelper = NOOPServerHelper.INSTANCE;
            }
        };

        if (useBuiltIn) {
            RegisterBuiltin.register(clientProviderFactory);
        }

        for (String providerToRegister : providersToRegister) {
            try {
                clientProviderFactory
                        .registerProvider(Thread.currentThread().getContextClassLoader().loadClass(providerToRegister.trim()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to find class for provider " + providerToRegister, e);
            }
        }

        RestClientBuilderImpl.PROVIDER_FACTORY = clientProviderFactory;
    }
}
