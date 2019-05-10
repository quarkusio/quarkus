/*
 * Copyright 2019 Red Hat, Inc.
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
package io.quarkus.keycloak;

import java.io.InputStream;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.undertow.KeycloakServletExtension;
import org.keycloak.representations.adapters.config.AdapterConfig;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Template;
import io.undertow.servlet.ServletExtension;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Template
public class KeycloakTemplate {

    public ServletExtension createServletExtension(AdapterDeploymentContext deploymentContext) {
        return new KeycloakServletExtension(deploymentContext);
    }

    public QuarkusDeploymentContext createKeycloakDeploymentContext(AdapterConfig defaultConfig) {
        KeycloakDeployment deployment;

        if (defaultConfig == null) {
            InputStream config = loadConfig(Thread.currentThread().getContextClassLoader());

            if (config == null) {
                config = loadConfig(getClass().getClassLoader());
            }

            deployment = KeycloakDeploymentBuilder.build(config);
        } else {
            deployment = KeycloakDeploymentBuilder.build(defaultConfig);
        }

        return new QuarkusDeploymentContext(deployment);
    }

    public BeanContainerListener createBeanContainerListener(QuarkusDeploymentContext deploymentContext) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                deploymentContext.setConfigResolver(container.instance(QuarkusKeycloakConfigResolver.class));
            }
        };
    }

    private InputStream loadConfig(ClassLoader classLoader) {
        return classLoader.getResourceAsStream("keycloak.json");
    }
}
