package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.strimzi.test.container.StrimziKafkaContainer

class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private StrimziKafkaContainer kafka = new StrimziKafkaContainer()

    String getBootstrapServers() {
        kafka.getBootstrapServers()
    }

    @Override
    Map<String, String> start() {
        kafka.start()
        ['kafka.bootstrap.servers': kafka.getBootstrapServers()]
    }

    @Override
    void stop() {
        kafka.close()
    }
}
