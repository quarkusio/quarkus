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

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * <p>
 * A {@link KeycloakConfigResolver} that is installed to applications so that any custom {@code KeycloakConfigResolver} instance
 * produced by applications are considered when resolving {@link KeycloakDeployment} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class QuarkusKeycloakConfigResolver implements KeycloakConfigResolver {

    @Inject
    Instance<KeycloakConfigResolver> configResolvers;

    private KeycloakConfigResolver delegate;
    private KeycloakDeployment defaultDeployment;

    @Override
    public KeycloakDeployment resolve(HttpFacade.Request facade) {
        if (delegate == null) {
            return defaultDeployment;
        }

        KeycloakDeployment deployment = delegate.resolve(facade);

        if (deployment == null) {
            deployment = defaultDeployment;
        }

        return deployment;
    }

    void init(KeycloakDeployment defaultDeployment) {
        this.defaultDeployment = defaultDeployment;
        delegate = configResolvers.stream()
                .filter(keycloakConfigResolver -> !QuarkusKeycloakConfigResolver.class.isInstance(keycloakConfigResolver))
                .findAny().orElse(null);
    }
}
