package io.quarkus.micrometer.runtime.binder.reactivemessaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.smallrye.reactive.messaging.observation.DefaultMessageObservation;
import io.smallrye.reactive.messaging.observation.MessageObservation;
import io.smallrye.reactive.messaging.observation.MessageObservationCollector;
import io.smallrye.reactive.messaging.observation.ObservationContext;

@ApplicationScoped
public class MicrometerObservationCollector
        implements MessageObservationCollector<MicrometerObservationCollector.MicrometerContext> {

    @Inject
    @ConfigProperty(name = "quarkus.messaging.observation.micrometer.enabled", defaultValue = "true")
    boolean enabled;

    @Override
    public MicrometerContext initObservation(String channel, boolean incoming, boolean emitter) {
        if (enabled) {
            return new MicrometerContext(channel);
        }
        return null;
    }

    @Override
    public MessageObservation onNewMessage(String channel, Message<?> message, MicrometerContext ctx) {
        ctx.msgCount.increment();
        return new DefaultMessageObservation(channel);
    }

    public static class MicrometerContext implements ObservationContext {
        final Counter msgCount;
        final Timer duration;
        final Counter acks;
        final Counter nacks;

        public MicrometerContext(String channel) {
            Tags tags = Tags.of(Tag.of("channel", channel));
            this.msgCount = Counter.builder("quarkus.messaging.message.count")
                    .description("The number of messages observed")
                    .tags(tags)
                    .register(Metrics.globalRegistry);
            this.duration = Timer.builder("quarkus.messaging.message.duration")
                    .description("The duration of the message processing")
                    .tags(tags)
                    .register(Metrics.globalRegistry);
            this.acks = Counter.builder("quarkus.messaging.message.acks")
                    .description("The number of messages processed successfully")
                    .tags(tags)
                    .register(Metrics.globalRegistry);
            this.nacks = Counter.builder("quarkus.messaging.message.failures")
                    .description("The number of messages processed with failures")
                    .tags(tags)
                    .register(Metrics.globalRegistry);
        }

        @Override
        public void complete(MessageObservation observation) {
            if (observation.getReason() == null) {
                acks.increment();
            } else {
                nacks.increment();
            }
            duration.record(observation.getCompletionDuration());
        }
    }
}
