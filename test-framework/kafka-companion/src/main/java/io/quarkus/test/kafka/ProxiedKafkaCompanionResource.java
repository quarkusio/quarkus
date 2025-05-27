package io.quarkus.test.kafka;

import java.util.Map;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.test.KafkaProxy;
import io.smallrye.reactive.messaging.kafka.companion.test.ProxiedStrimziKafkaContainer;
import io.strimzi.test.container.StrimziKafkaContainer;

public class ProxiedKafkaCompanionResource extends KafkaCompanionResource {

    private ProxiedStrimziKafkaContainer proxiedKafka;
    private KafkaProxy toxiProxy;
    private KafkaCompanion kafkaCompanion;

    @Override
    protected StrimziKafkaContainer createContainer(String imageName) {
        if (imageName == null) {
            proxiedKafka = new ProxiedStrimziKafkaContainer();
        } else {
            proxiedKafka = new ProxiedStrimziKafkaContainer(imageName);
        }
        return proxiedKafka;
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> config = super.start();
        if (proxiedKafka != null) {
            toxiProxy = proxiedKafka.getKafkaProxy();
            kafkaCompanion = new KafkaCompanion(toxiProxy.getProxyBootstrapServers());
        }
        return config;
    }

    @Override
    public void inject(TestInjector testInjector) {
        super.inject(testInjector);
        testInjector.injectIntoFields(this.toxiProxy,
                new TestInjector.AnnotatedAndMatchesType(InjectKafkaProxy.class, KafkaProxy.class));
        testInjector.injectIntoFields(this.kafkaCompanion,
                new TestInjector.AnnotatedAndMatchesType(InjectKafkaProxyCompanion.class, KafkaCompanion.class));
    }

}
