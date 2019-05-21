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

import org.apache.commons.logging.impl.Jdk14Logger;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.keycloak.adapters.authentication.ClientCredentialsProvider;
import org.keycloak.adapters.authentication.ClientIdAndSecretCredentialsProvider;
import org.keycloak.adapters.authentication.JWTClientCredentialsProvider;
import org.keycloak.adapters.authentication.JWTClientSecretCredentialsProvider;
import org.keycloak.adapters.authorization.ClaimInformationPointProviderFactory;
import org.keycloak.adapters.authorization.cip.ClaimsInformationPointProviderFactory;
import org.keycloak.adapters.authorization.cip.HttpClaimInformationPointProviderFactory;
import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.json.StringListMapDeserializer;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;

public class KeycloakReflectionProcessor {

    @BuildStep
    public void registerReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        reflectiveItems.produce(new ReflectiveClassBuildItem(true, true,
                JsonWebToken.class.getName(),
                JWSHeader.class.getName(),
                AccessToken.class.getName(),
                IDToken.class.getName(),
                RefreshToken.class.getName(),
                AccessTokenResponse.class.getName(),
                JSONWebKeySet.class.getName(),
                JWK.class.getName(),
                StringOrArrayDeserializer.class.getName(),
                AccessToken.Access.class.getName(),
                AccessToken.Authorization.class.getName(),
                AuthorizationRequest.class.getName(),
                AuthorizationResponse.class.getName(),
                PermissionRequest.class.getName(),
                PermissionResponse.class.getName(),
                PermissionTicketToken.class.getName(),
                Permission.class.getName(),
                ServerConfiguration.class.getName(),
                ResourceRepresentation.class.getName(),
                ScopeRepresentation.class.getName(),
                ResourceOwnerRepresentation.class.getName(),
                StringListMapDeserializer.class.getName(),
                StringOrArrayDeserializer.class.getName(),
                LogFactoryImpl.class.getName(),
                Jdk14Logger.class.getName()));
    }

    @BuildStep
    public void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem(ClientCredentialsProvider.class.getName(),
                ClientIdAndSecretCredentialsProvider.class.getName(),
                JWTClientCredentialsProvider.class.getName(),
                JWTClientSecretCredentialsProvider.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(ClaimInformationPointProviderFactory.class.getName(),
                HttpClaimInformationPointProviderFactory.class.getName(),
                ClaimsInformationPointProviderFactory.class.getName()));

    }
}
