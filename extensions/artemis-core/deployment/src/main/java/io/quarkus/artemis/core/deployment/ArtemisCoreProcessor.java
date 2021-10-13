package io.quarkus.artemis.core.deployment;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.apache.activemq.artemis.api.core.client.ServerLocator;
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

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.artemis.core.runtime.ArtemisCoreRecorder;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

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
    NativeImageConfigBuildItem config() {
        return NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("org.apache.activemq.artemis.api.core.ActiveMQBuffers")
                .addRuntimeInitializedClass("org.apache.activemq.artemis.utils.RandomUtil").build();
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
    HealthBuildItem health(ArtemisBuildTimeConfig buildConfig, Optional<ArtemisJmsBuildItem> artemisJms) {
        if (artemisJms.isPresent()) {
            return null;
        }

        return new HealthBuildItem(
                "io.quarkus.artemis.core.runtime.health.ServerLocatorHealthCheck",
                buildConfig.healthEnabled);
    }

    @BuildStep
    void load(BuildProducer<FeatureBuildItem> feature, Optional<ArtemisJmsBuildItem> artemisJms) {

        if (artemisJms.isPresent()) {
            return;
        }
        feature.produce(new FeatureBuildItem(Feature.ARTEMIS_CORE));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ArtemisCoreConfiguredBuildItem configure(ArtemisCoreRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer, Optional<ArtemisJmsBuildItem> artemisJms) {

        if (artemisJms.isPresent()) {
            return null;
        }

        SyntheticBeanBuildItem serverLocator = SyntheticBeanBuildItem.configure(ServerLocator.class)
                .supplier(recorder.getServerLocatorSupplier())
                .scope(ApplicationScoped.class)
                .defaultBean()
                .unremovable()
                .setRuntimeInit()
                .done();
        syntheticBeanProducer.produce(serverLocator);

        return new ArtemisCoreConfiguredBuildItem();
    }
}
