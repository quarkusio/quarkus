package io.quarkus.smallrye.reactivemessaging.runtime;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.extension.ChannelConfiguration;
import io.smallrye.reactive.messaging.extension.EmitterConfiguration;
import io.smallrye.reactive.messaging.extension.MediatorManager;

@Dependent
public class SmallRyeReactiveMessagingLifecycle {

    @Inject
    MediatorManager mediatorManager;

    void onStaticInit(@Observes @Initialized(ApplicationScoped.class) Object event,
            SmallRyeReactiveMessagingRecorder.SmallRyeReactiveMessagingContext context,
            QuarkusWorkerPoolRegistry workerPoolRegistry) {
        mediatorManager.addAnalyzed(context.getMediatorConfigurations());
        for (WorkerConfiguration worker : context.getWorkerConfigurations()) {
            workerPoolRegistry.defineWorker(worker.getClassName(), worker.getMethodName(), worker.getPoolName());
        }
        for (EmitterConfiguration emitter : context.getEmitterConfigurations()) {
            mediatorManager.addEmitter(emitter);
        }
        for (ChannelConfiguration channel : context.getChannelConfigurations()) {
            mediatorManager.addChannel(channel);
        }
    }

    void onApplicationStart(@Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE) StartupEvent event) {
        try {
            mediatorManager.start();
        } catch (Exception e) {
            if (e instanceof DeploymentException || e instanceof DefinitionException) {
                throw e;
            }
            throw new DeploymentException(e);
        }
    }

}
