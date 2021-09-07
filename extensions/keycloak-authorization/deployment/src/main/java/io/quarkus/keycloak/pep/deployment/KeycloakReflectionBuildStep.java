package io.quarkus.keycloak.pep.deployment;

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
import org.keycloak.protocol.oidc.representations.MTLSEndpointAliases;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
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
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class KeycloakReflectionBuildStep {

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
                MTLSEndpointAliases.class.getName(),
                OIDCConfigurationRepresentation.class.getName()));
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

    @BuildStep
    public void runtimeInit(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.keycloak.common.util.BouncyIntegration"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.keycloak.common.util.PemUtils"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.keycloak.common.util.DerUtils"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.keycloak.common.util.KeystoreUtil"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.keycloak.common.util.CertificateUtils"));
    }
}
