package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.reactive.messaging.extension.ChannelConfiguration;
import io.smallrye.reactive.messaging.extension.EmitterConfiguration;

@Recorder
public class SmallRyeReactiveMessagingRecorder {

    public Supplier<Object> createContext(List<QuarkusMediatorConfiguration> mediatorConfigurations,
            List<WorkerConfiguration> workerConfigurations, List<EmitterConfiguration> emitterConfigurations,
            List<ChannelConfiguration> channelConfigurations) {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                return new SmallRyeReactiveMessagingContext() {

                    @Override
                    public List<WorkerConfiguration> getWorkerConfigurations() {
                        return workerConfigurations;
                    }

                    @Override
                    public List<QuarkusMediatorConfiguration> getMediatorConfigurations() {
                        return mediatorConfigurations;
                    }

                    @Override
                    public List<EmitterConfiguration> getEmitterConfigurations() {
                        return emitterConfigurations;
                    }

                    @Override
                    public List<ChannelConfiguration> getChannelConfigurations() {
                        return channelConfigurations;
                    }
                };
            }
        };
    }

    public interface SmallRyeReactiveMessagingContext {

        List<EmitterConfiguration> getEmitterConfigurations();

        List<ChannelConfiguration> getChannelConfigurations();

        List<QuarkusMediatorConfiguration> getMediatorConfigurations();

        List<WorkerConfiguration> getWorkerConfigurations();

    }
}
