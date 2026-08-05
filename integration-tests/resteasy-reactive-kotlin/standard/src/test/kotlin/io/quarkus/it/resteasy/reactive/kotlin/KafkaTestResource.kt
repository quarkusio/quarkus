package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.strimzi.test.container.StrimziKafkaCluster

class KafkaTestResource : QuarkusTestResourceLifecycleManager {

    private val kafka: StrimziKafkaCluster =
        StrimziKafkaCluster.StrimziKafkaClusterBuilder().build()

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
