package io.quarkus.it.kafka;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.server.KeycloakContainer;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaKeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private StrimziKafkaContainer kafka;
    private KeycloakContainer keycloak;
    private static final String KEYCLOAK_REALM_JSON = System.getProperty("keycloak.realm.json");

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = new HashMap<>();

        //Start keycloak container
        keycloak = new KeycloakContainer();
        keycloak.start();
        KeycloakTestClient client = new KeycloakTestClient(keycloak.getServerUrl());
        client.createRealmFromPath(KEYCLOAK_REALM_JSON);

        //Start kafka container
        this.kafka = new StrimziKafkaContainer("quay.io/strimzi/kafka:latest-kafka-3.7.0")
                .withBrokerId(1)
                .withKafkaConfigurationMap(Map.of("listener.security.protocol.map",
                        "JWT:SASL_PLAINTEXT,BROKER1:PLAINTEXT",
                        "listener.name.jwt.oauthbearer.sasl.jaas.config",
                        getOauthSaslJaasConfig(keycloak.getInternalUrl(), keycloak.getServerUrl()),
                        "listener.name.jwt.plain.sasl.jaas.config",
                        getPlainSaslJaasConfig(keycloak.getInternalUrl(), keycloak.getServerUrl())))
                .withNetworkAliases("kafka")
                .withServerProperties(MountableFile.forClasspathResource("kafkaServer.properties"))
                .withBootstrapServers(
                        c -> String.format("JWT://%s:%s", c.getHost(), c.getMappedPort(KAFKA_PORT)));
        this.kafka.start();
        properties.put("kafka.bootstrap.servers", this.kafka.getBootstrapServers());
        properties.put("mp.messaging.connector.smallrye-kafka.sasl.jaas.config",
                getClientSaslJaasConfig(keycloak.getServerUrl()));

        return properties;
    }

    private String getClientSaslJaasConfig(String keycloakServerUrl) {
        return "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required" +
                " oauth.client.id=\"kafka-client\"" +
                " oauth.client.secret=\"kafka-client-secret\"" +
                " oauth.token.endpoint.uri=\"" + keycloakServerUrl + "/realms/kafka-authz/protocol/openid-connect/token\";";
    }

    private String getPlainSaslJaasConfig(String keycloakInternalUrl, String keycloakServerUrl) {
        return "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "oauth.jwks.endpoint.uri=\"" + keycloakInternalUrl + "/realms/kafka-authz/protocol/openid-connect/certs\" " +
                "oauth.valid.issuer.uri=\"" + keycloakServerUrl + "/realms/kafka-authz\" " +
                "oauth.token.endpoint.uri=\"" + keycloakInternalUrl + "/realms/kafka-authz/protocol/openid-connect/token\" " +
                "oauth.client.id=\"kafka\" " +
                "oauth.client.secret=\"kafka-secret\" " +
                "unsecuredLoginStringClaim_sub=\"admin\";";
    }

    private String getOauthSaslJaasConfig(String keycloakInternalUrl, String keycloakServerUrl) {
        return "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
                "oauth.jwks.endpoint.uri=\"" + keycloakInternalUrl + "/realms/kafka-authz/protocol/openid-connect/certs\" " +
                "oauth.valid.issuer.uri=\"" + keycloakServerUrl + "/realms/kafka-authz\" " +
                "oauth.token.endpoint.uri=\"" + keycloakInternalUrl + "/realms/kafka-authz/protocol/openid-connect/token\" " +
                "oauth.client.id=\"kafka\" " +
                "oauth.client.secret=\"kafka-secret\";";
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
        if (keycloak != null) {
            keycloak.stop();
        }
    }
}
