package io.quarkus.kafka.client.deployment;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

public class StrimziOAuthProcessor {

    @BuildStep
    public void handleStrimziOAuth(CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime("io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler")) {
            return;
        }

        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(
                        "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler")
                        .methods().fields().build());

        if (curateOutcomeBuildItem.getApplicationModel().getDependencies().stream().anyMatch(
                x -> x.getGroupId().equals("org.keycloak") && x.getArtifactId().equals("keycloak-core"))) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder("org.keycloak.jose.jws.JWSHeader",
                    "org.keycloak.representations.AccessToken",
                    "org.keycloak.representations.AccessToken$Access",
                    "org.keycloak.representations.AccessTokenResponse",
                    "org.keycloak.representations.IDToken",
                    "org.keycloak.representations.JsonWebToken",
                    "org.keycloak.jose.jwk.JSONWebKeySet",
                    "org.keycloak.jose.jwk.JWK",
                    "org.keycloak.json.StringOrArrayDeserializer",
                    "org.keycloak.json.StringListMapDeserializer").methods().fields().build());
        }
    }

}
