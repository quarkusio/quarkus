package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.strimzi.StrimziKafkaContainer

class KafkaTestResource : QuarkusTestResourceLifecycleManager {

    private val kafka: StrimziKafkaContainer = StrimziKafkaContainer()

    fun getBootstrapServers(): String? {
        return kafka.getBootstrapServers()
    }

    override fun start(): Map<String, String>? {
        kafka.start()
        return mapOf("kafka.bootstrap.servers" to kafka.getBootstrapServers())
    }

    override fun stop() {
        kafka.close()
    }
}