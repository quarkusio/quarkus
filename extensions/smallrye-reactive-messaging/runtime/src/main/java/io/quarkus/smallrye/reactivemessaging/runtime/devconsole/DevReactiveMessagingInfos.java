package io.quarkus.smallrye.reactivemessaging.runtime.devconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder.SmallRyeReactiveMessagingContext;
import io.smallrye.reactive.messaging.MediatorConfiguration;
import io.smallrye.reactive.messaging.extension.ChannelConfiguration;
import io.smallrye.reactive.messaging.extension.EmitterConfiguration;

public class DevReactiveMessagingInfos {

    // We must use a lazy value because MediatorManager is initialized when the app starts 
    private final LazyValue<List<DevChannelInfo>> channels;

    public DevReactiveMessagingInfos() {
        this.channels = new LazyValue<>(new Supplier<List<DevChannelInfo>>() {

            @Override
            public List<DevChannelInfo> get() {
                ArcContainer container = Arc.container();
                SmallRyeReactiveMessagingContext context = container.instance(SmallRyeReactiveMessagingContext.class)
                        .get();

                // collect all channels
                Map<String, Component> publishers = new HashMap<>();
                Map<String, List<Component>> consumers = new HashMap<>();
                Function<String, List<Component>> fun = e -> new ArrayList<>();

                // Unfortunately, there is no easy way to obtain the connectors metadata
                Connectors connectors = container.instance(Connectors.class).get();
                for (Entry<String, Component> entry : connectors.outgoingConnectors.entrySet()) {
                    publishers.put(entry.getKey(), entry.getValue());
                }
                for (Entry<String, Component> entry : connectors.incomingConnectors.entrySet()) {
                    consumers.computeIfAbsent(entry.getKey(), fun)
                            .add(entry.getValue());
                }

                for (EmitterConfiguration emitter : context.getEmitterConfigurations()) {
                    publishers.put(emitter.name,
                            new Component(ComponentType.EMITTER,
                                    emitter.broadcast ? "<span class=\"annotation\">&#64;Broadcast</span> "
                                            : "" + asCode(DevConsoleRecorder.EMITTERS.get(emitter.name))));
                }
                for (ChannelConfiguration channel : context.getChannelConfigurations()) {
                    consumers.computeIfAbsent(channel.channelName, fun)
                            .add(new Component(ComponentType.CHANNEL,
                                    asCode(DevConsoleRecorder.CHANNELS.get(channel.channelName))));
                }
                for (MediatorConfiguration mediator : context.getMediatorConfigurations()) {
                    boolean isProcessor = !mediator.getIncoming().isEmpty() && mediator.getOutgoing() != null;
                    if (isProcessor) {
                        publishers.put(mediator.getOutgoing(),
                                new Component(ComponentType.PROCESSOR, asMethod(mediator.methodAsString())));
                        for (String incoming : mediator.getIncoming()) {
                            consumers.computeIfAbsent(incoming, fun)
                                    .add(new Component(ComponentType.PROCESSOR,
                                            asMethod(mediator.methodAsString())));
                        }
                    } else if (mediator.getOutgoing() != null) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(asMethod(mediator.methodAsString()));
                        if (mediator.getBroadcast()) {
                            builder.append("[broadcast: true]");
                        }
                        publishers.put(mediator.getOutgoing(),
                                new Component(ComponentType.PUBLISHER, builder.toString()));
                    } else if (!mediator.getIncoming().isEmpty()) {
                        for (String incoming : mediator.getIncoming()) {
                            consumers.computeIfAbsent(incoming, fun)
                                    .add(new Component(ComponentType.SUBSCRIBER,
                                            asMethod(mediator.methodAsString())));
                        }
                    }
                }

                Set<String> channels = new HashSet<>();
                channels.addAll(publishers.keySet());
                channels.addAll(consumers.keySet());

                List<DevChannelInfo> infos = new ArrayList<>();
                for (String channel : channels) {
                    infos.add(new DevChannelInfo(channel, publishers.get(channel), consumers.get(channel)));
                }
                Collections.sort(infos);
                return infos;
            }
        });
    }

    static String asMethod(String value) {
        return asCode(value + "()");
    }

    static String asCode(String value) {
        return "<code>" + value + "</code>";
    }

    public List<DevChannelInfo> getChannels() {
        return channels.get();
    }

    public static class DevChannelInfo implements Comparable<DevChannelInfo> {

        private final String name;
        private final Component publisher;
        private final List<Component> consumers;

        public DevChannelInfo(String name, Component publisher, List<Component> consumers) {
            this.name = name;
            this.publisher = publisher;
            this.consumers = consumers;
        }

        public String getName() {
            return name;
        }

        public Component getPublisher() {
            return publisher;
        }

        public List<Component> getConsumers() {
            return consumers;
        }

        @Override
        public int compareTo(DevChannelInfo other) {
            // publisher connectors first
            if (publisher.type != other.publisher.type) {
                return publisher.type == ComponentType.CONNECTOR ? -1 : 1;
            }
            // consumer connectors last
            long consumerConnetors = consumers.stream().filter(Component::isConnector).count();
            long otherConsumersConnectors = other.consumers.stream().filter(Component::isConnector).count();
            if (consumerConnetors != otherConsumersConnectors) {
                return Long.compare(otherConsumersConnectors, consumerConnetors);
            }

            if (publisher.type == ComponentType.CONNECTOR && other.publisher.type != ComponentType.CONNECTOR) {
                return 1;
            }
            // alphabetically
            return name.compareTo(other.name);
        }

    }

    public static class Component {

        public final ComponentType type;
        public final String description;

        public Component(ComponentType type, String description) {
            this.type = type;
            this.description = description;
        }

        boolean isConnector() {
            return type == ComponentType.CONNECTOR;
        }

    }

    public enum ComponentType {
        CONNECTOR,
        PROCESSOR,
        PUBLISHER,
        EMITTER,
        CHANNEL,
        SUBSCRIBER,
    }

}
