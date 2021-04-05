package io.quarkus.keycloak.pep.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class KeycloakReflectionBuildStep {

    @BuildStep
    public void registerReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        reflectiveItems.produce(new ReflectiveClassBuildItem(true, true,
                "org.keycloak.authorization.client.representation.ServerConfiguration",
                "org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation",
                "org.keycloak.jose.jwk.JSONWebKeySet",
                "org.keycloak.jose.jwk.JWK",
                "org.keycloak.jose.jws.JWSHeader",
                "org.keycloak.json.StringOrArrayDeserializer",
                "org.keycloak.json.StringListMapDeserializer",
                "org.keycloak.representations.AccessToken",
                "org.keycloak.representations.AccessTokenResponse",
                "org.keycloak.representations.AccessToken$Access",
                "org.keycloak.representations.AccessToken$Authorization",
                "org.keycloak.representations.IDToken",
                "org.keycloak.representations.JsonWebToken",
                "org.keycloak.representations.RefreshToken",
                "org.keycloak.representations.idm.authorization.AuthorizationRequest",
                "org.keycloak.representations.idm.authorization.AuthorizationResponse",
                "org.keycloak.representations.idm.authorization.PermissionRequest",
                "org.keycloak.representations.idm.authorization.PermissionResponse",
                "org.keycloak.representations.idm.authorization.PermissionTicketToken",
                "org.keycloak.representations.idm.authorization.Permission",
                "org.keycloak.representations.idm.authorization.ResourceRepresentation",
                "org.keycloak.representations.idm.authorization.ScopeRepresentation",
                "org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation"));
    }

    @BuildStep
    public void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem("org.keycloak.adapters.authentication.ClientCredentialsProvider",
                "org.keycloak.adapters.authentication.ClientIdAndSecretCredentialsProvider",
                "org.keycloak.adapters.authentication.JWTClientCredentialsProvider",
                "org.keycloak.adapters.authentication.JWTClientSecretCredentialsProvider"));
        serviceProvider.produce(
                new ServiceProviderBuildItem("org.keycloak.adapters.authorization.ClaimInformationPointProviderFactory",
                        "org.keycloak.adapters.authorization.cip.HttpClaimInformationPointProviderFactory",
                        "org.keycloak.adapters.authorization.cip.ClaimsInformationPointProviderFactory"));

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
