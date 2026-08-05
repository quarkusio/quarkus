package io.quarkus.test.kafka;

import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.Map;

import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import eu.rekawek.toxiproxy.Proxy;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.test.KafkaProxy;
import io.strimzi.test.container.StrimziKafkaCluster;
import io.strimzi.test.container.StrimziKafkaContainer;

public class ProxiedKafkaCompanionResource extends KafkaCompanionResource {

    private StrimziKafkaCluster cluster;
    private StrimziKafkaContainer proxiedKafka;
    private KafkaProxy toxiProxy;

    @Override
    protected StrimziKafkaContainer createContainer(String imageName) {
        ToxiproxyContainer toxiproxy = new ToxiproxyContainer(
                DockerImageName.parse(System.getProperty("toxiproxy.image.name", "ghcr.io/shopify/toxiproxy:2.4.0"))
                        .asCompatibleSubstituteFor("shopify/toxiproxy"))
                .withNetworkAliases("toxiproxy");
        cluster = new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
                .withProxyContainer(toxiproxy)
                .build();
        proxiedKafka = cluster.getBrokers().stream().findFirst().get();
        return proxiedKafka;
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> config;
        if (kafkaCompanion == null && kafka != null) {
            cluster.start();
            await().until(kafka::isRunning);
            kafkaCompanion = new KafkaCompanion(cluster.getBootstrapServers());
            config = Collections.singletonMap("kafka.bootstrap.servers", kafka.getBootstrapServers());
        } else {
            config = Collections.emptyMap();
        }
        if (proxiedKafka != null) {
            Proxy proxyForNode = cluster.getProxyForNode(proxiedKafka.getNodeId());
            toxiProxy = new KafkaProxy(proxyForNode, proxiedKafka.getHost(), kafka.getMappedPort(9092), 0);
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
