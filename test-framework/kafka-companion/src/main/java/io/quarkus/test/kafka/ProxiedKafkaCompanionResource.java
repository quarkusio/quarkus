package io.quarkus.test.kafka;

import java.util.Map;

import io.smallrye.reactive.messaging.kafka.companion.test.KafkaProxy;
import io.smallrye.reactive.messaging.kafka.companion.test.ProxiedStrimziKafkaContainer;
import io.strimzi.test.container.StrimziKafkaContainer;

public class ProxiedKafkaCompanionResource extends KafkaCompanionResource {

    private ProxiedStrimziKafkaContainer proxiedKafka;
    private KafkaProxy toxiProxy;

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
        }
        return config;
    }

    @Override
    public void inject(TestInjector testInjector) {
        super.inject(testInjector);
        testInjector.injectIntoFields(this.toxiProxy,
                new TestInjector.AnnotatedAndMatchesType(InjectKafkaProxy.class, KafkaProxy.class));
    }

}
