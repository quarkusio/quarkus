package io.quarkus.kafka.streams.runtime;

import java.util.Optional;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.LaunchMode;

public class KafkaStreamsPropertiesUtil {

    private static final String STREAMS_OPTION_PREFIX = "kafka-streams.";
    private static final String QUARKUS_STREAMS_OPTION_PREFIX = "quarkus." + STREAMS_OPTION_PREFIX;

    private static boolean isKafkaStreamsProperty(String prefix, String property) {
        return property.startsWith(prefix);
    }

    private static void includeKafkaStreamsProperty(Config config, Properties kafkaStreamsProperties, String prefix,
            String property) {
        Optional<String> value = config.getOptionalValue(property, String.class);
        if (value.isPresent()) {
            kafkaStreamsProperties.setProperty(property.substring(prefix.length()), value.get());
        }
    }

    private static void addHotReplacementInterceptor(Properties kafkaStreamsProperties) {
        String interceptorConfig = HotReplacementInterceptor.class.getName();
        Object originalInterceptorConfig = kafkaStreamsProperties
                .get(StreamsConfig.consumerPrefix(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG));

        if (originalInterceptorConfig != null) {
            interceptorConfig = interceptorConfig + "," + originalInterceptorConfig;
        }

        kafkaStreamsProperties.put(StreamsConfig.consumerPrefix(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG), interceptorConfig);
    }

    private static Properties kafkaStreamsProperties(String prefix) {
        Properties kafkaStreamsProperties = new Properties();
        Config config = ConfigProvider.getConfig();
        for (String property : config.getPropertyNames()) {
            if (isKafkaStreamsProperty(prefix, property)) {
                includeKafkaStreamsProperty(config, kafkaStreamsProperties, prefix, property);
            }
        }

        return kafkaStreamsProperties;
    }

    public static Properties appKafkaStreamsProperties() {
        return kafkaStreamsProperties(STREAMS_OPTION_PREFIX);
    }

    public static Properties quarkusKafkaStreamsProperties() {
        return kafkaStreamsProperties(QUARKUS_STREAMS_OPTION_PREFIX);
    }

    public static Properties buildKafkaStreamsProperties(LaunchMode launchMode) {
        Properties kafkaStreamsProperties = appKafkaStreamsProperties();

        if (launchMode == LaunchMode.DEVELOPMENT) {
            addHotReplacementInterceptor(kafkaStreamsProperties);
        }

        return kafkaStreamsProperties;
    }

}
