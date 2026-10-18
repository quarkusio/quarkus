package io.quarkus.smallrye.reactivemessaging.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.smallrye.reactive.messaging.ChannelRegistry;
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.EmitterFactory;
import io.smallrye.reactive.messaging.annotations.EmitterFactoryFor;
import io.smallrye.reactive.messaging.providers.extension.ChannelProducer;

@EmitterFactoryFor(ContextualEmitter.class)
@ApplicationScoped
public class ContextualEmitterFactory implements EmitterFactory<ContextualEmitterImpl<Object>> {

    @Inject
    ChannelRegistry channelRegistry;

    @Override
    public ContextualEmitterImpl<Object> createEmitter(EmitterConfiguration emitterConfiguration, long l) {
        return new ContextualEmitterImpl<>(emitterConfiguration, l);
    }

    @Produces
    @Typed(ContextualEmitter.class)
    @Channel("") // Stream name is ignored during type-safe resolution
    <T> ContextualEmitter<T> produceEmitter(InjectionPoint injectionPoint) {
        String channelName = ChannelProducer.getChannelName(injectionPoint);
        return channelRegistry.getEmitter(channelName, ContextualEmitter.class);
    }

}
