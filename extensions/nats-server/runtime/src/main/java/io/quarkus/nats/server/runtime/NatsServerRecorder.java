package io.quarkus.nats.server.runtime;

import berlin.yuna.natsserver.logic.Nats;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NatsServerRecorder {

    public BeanContainerListener setLiquibaseConfig(final NatsServerConfig natsServerConfig) {
        return beanContainer -> {
            final NatsServerProducer producer = beanContainer.beanInstance(NatsServerProducer.class);
            producer.setNatsServerConfig(natsServerConfig);
        };
    }

    public void migrate(final BeanContainer container) {
        final Nats nats = container.beanInstance(Nats.class);
    }
}
