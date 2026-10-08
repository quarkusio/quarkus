package io.quarkus.kafka.streams.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.Test;

/**
 * The admin client used to wait for the topics must see the config providers configured for the streams application,
 * as real entries and not only as {@link Properties} defaults, and the {@code admin.} prefixed variants must win.
 */
class KafkaStreamsProducerAdminConfigTest {

    @Test
    void configProvidersAreCopiedToTheAdminClientConfig() {
        Properties properties = new Properties();
        properties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty("config.providers", "secrets");
        properties.setProperty("config.providers.secrets.class", "io.example.SecretProvider");
        properties.setProperty("config.providers.secrets.param.namespace", "apps");
        properties.setProperty("ssl.truststore.certificates", "${secrets:apps/kafka-ca:ca.crt}");

        Properties adminConfig = KafkaStreamsProducer.getAdminClientConfig(properties);

        assertThat(adminConfig.containsKey("config.providers")).isTrue();
        assertThat(adminConfig.containsKey("config.providers.secrets.class")).isTrue();
        assertThat(adminConfig.get("config.providers.secrets.class")).isEqualTo("io.example.SecretProvider");
        assertThat(adminConfig.containsKey("config.providers.secrets.param.namespace")).isTrue();
        assertThat(adminConfig.get("config.providers.secrets.param.namespace")).isEqualTo("apps");
        assertThat(adminConfig.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092");
    }

    @Test
    void adminPrefixedConfigProvidersWin() {
        Properties properties = new Properties();
        properties.setProperty("config.providers", "secrets");
        properties.setProperty("config.providers.secrets.class", "io.example.SecretProvider");
        properties.setProperty("admin.config.providers", "admin-secrets");
        properties.setProperty("admin.config.providers.admin-secrets.class", "io.example.AdminSecretProvider");
        properties.setProperty("admin.config.providers.admin-secrets.param.namespace", "admin");

        Properties adminConfig = KafkaStreamsProducer.getAdminClientConfig(properties);

        assertThat(adminConfig.get("config.providers")).isEqualTo("admin-secrets");
        assertThat(adminConfig.get("config.providers.admin-secrets.class")).isEqualTo("io.example.AdminSecretProvider");
        assertThat(adminConfig.get("config.providers.admin-secrets.param.namespace")).isEqualTo("admin");
        assertThat(adminConfig.get("config.providers.secrets.class")).isEqualTo("io.example.SecretProvider");
        assertThat(adminConfig.containsKey("admin.config.providers")).isFalse();
    }
}
