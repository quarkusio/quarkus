package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.reactive.messaging.extension.ChannelConfiguration;
import io.smallrye.reactive.messaging.extension.EmitterConfiguration;
import io.smallrye.reactive.messaging.extension.MediatorManager;

/**
 * @author Martin Kouba
 */
@Recorder
public class SmallRyeReactiveMessagingRecorder {

    public void configureEmitter(BeanContainer container, EmitterConfiguration ec) {
        MediatorManager mediatorManager = container.instance(MediatorManager.class);
        mediatorManager.addEmitter(ec);
    }

    public void configureChannel(BeanContainer container, ChannelConfiguration cc) {
        MediatorManager mediatorManager = container.instance(MediatorManager.class);
        mediatorManager.addChannel(cc);
    }

    public void registerMediators(List<QuarkusMediatorConfiguration> configurations, BeanContainer container) {
        MediatorManager mediatorManager = container.instance(MediatorManager.class);
        mediatorManager.addAnalyzed(configurations);
    }

    public void configureWorkerPool(BeanContainer container, String className, String name, String poolName) {
        QuarkusWorkerPoolRegistry registry = container.instance(QuarkusWorkerPoolRegistry.class);
        registry.defineWorker(className, name, poolName);
    }
}
