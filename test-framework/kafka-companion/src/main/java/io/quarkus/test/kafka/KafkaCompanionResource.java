package io.quarkus.test.kafka;

import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaCompanionResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    public static String STRIMZI_KAFKA_IMAGE_KEY = "strimzi.kafka.image";
    public static String KAFKA_PORT_KEY = "kafka.port";
    public static String KRAFT_KEY = "kraft";

    protected String strimziKafkaContainerImage;
    protected Integer kafkaPort;
    protected boolean kraft;

    protected StrimziKafkaContainer kafka;
    protected KafkaCompanion kafkaCompanion;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        Map<String, String> devServicesProperties = context.devServicesProperties();
        String bootstrapServers = devServicesProperties.get("kafka.bootstrap.servers");
        if (bootstrapServers != null) {
            kafkaCompanion = new KafkaCompanion(bootstrapServers);
            String apicurioUrl = devServicesProperties
                    .get("mp.messaging.connector.smallrye-kafka.apicurio.registry.url");
            if (apicurioUrl != null) {
                // normally, the processor will set both property so it's safe to unconditionally load the confluent URL
                String confluentUrl = devServicesProperties
                        .get("mp.messaging.connector.smallrye-kafka.schema.registry.url");
                kafkaCompanion.setCommonClientConfig(Map.of("apicurio.registry.url", apicurioUrl,
                        "apicurio.registry.auto-register", "true", "schema.registry.url", confluentUrl));
            }
        }
    }

    protected StrimziKafkaContainer createContainer(String imageName) {
        if (imageName == null) {
            return new StrimziKafkaContainer();
        } else {
            return new StrimziKafkaContainer(imageName);
        }
    }

    @Override
    public void init(Map<String, String> initArgs) {
        if (kafkaCompanion == null) {
            strimziKafkaContainerImage = initArgs.get(STRIMZI_KAFKA_IMAGE_KEY);
            String portString = initArgs.get(KAFKA_PORT_KEY);
            kafkaPort = portString == null ? null : Integer.parseInt(portString);
            kraft = Boolean.parseBoolean(initArgs.get(KRAFT_KEY));
            kafka = createContainer(strimziKafkaContainerImage);
            if (kraft) {
                kafka.withBrokerId(1).withKraft();
            }
            if (kafkaPort != null) {
                kafka.withPort(kafkaPort);
            }
            Map<String, String> configMap = new HashMap<>(initArgs);
            configMap.remove(STRIMZI_KAFKA_IMAGE_KEY);
            configMap.remove(KAFKA_PORT_KEY);
            configMap.remove(KRAFT_KEY);
            kafka.withKafkaConfigurationMap(configMap);
        }
    }

    @Override
    public Map<String, String> start() {
        if (kafkaCompanion == null && kafka != null) {
            kafka.start();
            await().until(kafka::isRunning);
            kafkaCompanion = new KafkaCompanion(kafka.getBootstrapServers());
            return Collections.singletonMap("kafka.bootstrap.servers", kafka.getBootstrapServers());
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public void stop() {
        if (kafkaCompanion != null) {
            kafkaCompanion.close();
        }
        if (kafka != null) {
            kafka.close();
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(this.kafkaCompanion,
                new TestInjector.AnnotatedAndMatchesType(InjectKafkaCompanion.class, KafkaCompanion.class));
    }
}
