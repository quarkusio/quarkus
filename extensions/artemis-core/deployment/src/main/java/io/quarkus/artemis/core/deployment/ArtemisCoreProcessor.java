package io.quarkus.artemis.core.deployment;

import java.util.Collection;
import java.util.Optional;

import org.apache.activemq.artemis.api.core.client.loadbalance.ConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.api.core.client.loadbalance.FirstElementConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.api.core.client.loadbalance.RandomConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.api.core.client.loadbalance.RandomStickyConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.api.core.client.loadbalance.RoundRobinConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.spi.core.remoting.ConnectorFactory;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.artemis.core.runtime.ArtemisCoreProducer;
import io.quarkus.artemis.core.runtime.ArtemisCoreRecorder;
import io.quarkus.artemis.core.runtime.ArtemisRuntimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;

public class ArtemisCoreProcessor {

    private static final Logger LOGGER = Logger.getLogger(ArtemisCoreProcessor.class);

    static final Class[] BUILTIN_CONNECTOR_FACTORIES = {
            NettyConnectorFactory.class
    };

    static final Class[] BUILTIN_LOADBALANCING_POLICIES = {
            FirstElementConnectionLoadBalancingPolicy.class,
            RandomConnectionLoadBalancingPolicy.class,
            RandomStickyConnectionLoadBalancingPolicy.class,
            RoundRobinConnectionLoadBalancingPolicy.class
    };

    @BuildStep
    SubstrateConfigBuildItem config() {
        return SubstrateConfigBuildItem.builder()
                .addRuntimeInitializedClass("org.apache.activemq.artemis.api.core.ActiveMQBuffers").build();
    }

    @BuildStep
    void build(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        Collection<ClassInfo> connectorFactories = indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(ConnectorFactory.class.getName()));

        for (ClassInfo ci : connectorFactories) {
            LOGGER.debug("Adding reflective class " + ci);
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ci.toString()));
        }

        for (Class c : BUILTIN_CONNECTOR_FACTORIES) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, c));
        }

        Collection<ClassInfo> loadBalancers = indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(ConnectionLoadBalancingPolicy.class.getName()));

        for (ClassInfo ci : loadBalancers) {
            LOGGER.debug("Adding reflective class " + ci);
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ci.toString()));
        }

        for (Class c : BUILTIN_LOADBALANCING_POLICIES) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, c));
        }
    }

    @BuildStep
    void load(BuildProducer<AdditionalBeanBuildItem> additionalBean, BuildProducer<FeatureBuildItem> feature,
            Optional<ArtemisJmsBuildItem> artemisJms) {

        if (artemisJms.isPresent()) {
            return;
        }
        feature.produce(new FeatureBuildItem(FeatureBuildItem.ARTEMIS_CORE));
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(ArtemisCoreProducer.class));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configure(ArtemisCoreRecorder recorder, ArtemisRuntimeConfig runtimeConfig,
            BeanContainerBuildItem beanContainer, Optional<ArtemisJmsBuildItem> artemisJms) {

        if (artemisJms.isPresent()) {
            return;
        }
        recorder.setConfig(runtimeConfig, beanContainer.getValue());
    }
}
