package io.quarkus.kafka.streams.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;

class KafkaStreamsProducerTest {

    @Test
    void adminClientConfigShouldCopyConfigProviders() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put("config.providers", "secrets");
        props.put("config.providers.secrets.class", "io.strimzi.kafka.KubernetesSecretConfigProvider");

        Properties adminConfig = KafkaStreamsProducer.getAdminClientConfig(props);

        assertThat(adminConfig.getProperty("config.providers")).isEqualTo("secrets");
        assertThat(adminConfig.getProperty("config.providers.secrets.class"))
                .isEqualTo("io.strimzi.kafka.KubernetesSecretConfigProvider");
    }

    @Test
    void adminClientConfigShouldPreferAdminPrefixedConfigProviders() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put("config.providers", "secrets");
        props.put("config.providers.secrets.class", "io.strimzi.kafka.KubernetesSecretConfigProvider");
        // admin-prefixed should take precedence
        props.put(StreamsConfig.ADMIN_CLIENT_PREFIX + "config.providers", "file");
        props.put(StreamsConfig.ADMIN_CLIENT_PREFIX + "config.providers.file.class",
                "org.apache.kafka.common.config.provider.FileConfigProvider");

        Properties adminConfig = KafkaStreamsProducer.getAdminClientConfig(props);

        // admin-prefixed config providers should win
        assertThat(adminConfig.getProperty("config.providers")).isEqualTo("file");
        assertThat(adminConfig.getProperty("config.providers.file.class"))
                .isEqualTo("org.apache.kafka.common.config.provider.FileConfigProvider");
    }

    @Test
    void adminClientConfigShouldCopyKnownAdminConfigs() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        Properties adminConfig = KafkaStreamsProducer.getAdminClientConfig(props);

        assertThat(adminConfig.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG))
                .isEqualTo("localhost:9092");
        assertThat(adminConfig.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG))
                .isEqualTo("SSL");
    }

    @Test
    void adminClientConfigShouldPreferAdminPrefixedKnownConfigs() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.ADMIN_CLIENT_PREFIX + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                "admin-host:9093");

        Properties adminConfig = KafkaStreamsProducer.getAdminClientConfig(props);

        assertThat(adminConfig.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG))
                .isEqualTo("admin-host:9093");
    }

    @Test
    void adminClientConfigShouldNotCopyConfigProvidersWhenAdminPrefixedExists() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        // non-prefixed config provider
        props.put("config.providers", "secrets");
        // admin-prefixed version of the same property exists
        props.put(StreamsConfig.ADMIN_CLIENT_PREFIX + "config.providers", "file");

        Properties adminConfig = KafkaStreamsProducer.getAdminClientConfig(props);

        // admin-prefixed should win, non-prefixed should NOT be copied
        assertThat(adminConfig.getProperty("config.providers")).isEqualTo("file");
    }
}
