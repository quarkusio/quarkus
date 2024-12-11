package io.quarkus.smallrye.reactivemessaging.runtime.dev.ui;

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
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.MediatorConfiguration;
import io.smallrye.reactive.messaging.providers.extension.ChannelConfiguration;

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
                Map<String, List<Component>> publishers = new HashMap<>();
                Map<String, List<Component>> consumers = new HashMap<>();
                Function<String, List<Component>> fun = e -> new ArrayList<>();

                // Unfortunately, there is no easy way to obtain the connectors metadata
                Connectors connectors = container.instance(Connectors.class).get();
                for (Entry<String, Component> entry : connectors.outgoingConnectors.entrySet()) {
                    publishers.computeIfAbsent(entry.getKey(), fun)
                            .add(entry.getValue());
                }
                for (Entry<String, Component> entry : connectors.incomingConnectors.entrySet()) {
                    consumers.computeIfAbsent(entry.getKey(), fun)
                            .add(entry.getValue());
                }

                for (EmitterConfiguration emitter : context.getEmitterConfigurations()) {
                    publishers.computeIfAbsent(emitter.name(), fun)
                            .add(new Component(ComponentType.EMITTER,
                                    emitter.broadcast() ? "<span class=\"annotation\">&#64;Broadcast</span> "
                                            : "" + asCode(DevConsoleRecorder.EMITTERS.get(emitter.name()))));
                }
                for (ChannelConfiguration channel : context.getChannelConfigurations()) {
                    consumers.computeIfAbsent(channel.channelName, fun)
                            .add(new Component(ComponentType.CHANNEL,
                                    asCode(DevConsoleRecorder.CHANNELS.get(channel.channelName))));
                }
                for (MediatorConfiguration mediator : context.getMediatorConfigurations()) {
                    boolean isProcessor = !mediator.getIncoming().isEmpty() && !mediator.getOutgoings().isEmpty();
                    if (isProcessor) {
                        for (String outgoing : mediator.getOutgoings()) {
                            publishers.computeIfAbsent(outgoing, fun)
                                    .add(new Component(ComponentType.PROCESSOR, asMethod(mediator.methodAsString())));
                        }
                        for (String incoming : mediator.getIncoming()) {
                            consumers.computeIfAbsent(incoming, fun)
                                    .add(new Component(ComponentType.PROCESSOR,
                                            asMethod(mediator.methodAsString())));
                        }
                    } else if (!mediator.getOutgoings().isEmpty()) {
                        for (String outgoing : mediator.getOutgoings()) {
                            StringBuilder builder = new StringBuilder();
                            builder.append(asMethod(mediator.methodAsString()));
                            if (mediator.getBroadcast()) {
                                builder.append("[broadcast: true]");
                            }
                            publishers.computeIfAbsent(outgoing, fun)
                                    .add(new Component(ComponentType.PUBLISHER, builder.toString()));
                        }
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
        private final List<Component> publishers;
        private final List<Component> consumers;

        public DevChannelInfo(String name, List<Component> publishers, List<Component> consumers) {
            this.name = name;
            this.publishers = publishers != null ? publishers : Collections.emptyList();
            this.consumers = consumers != null ? consumers : Collections.emptyList();
        }

        public String getName() {
            return name;
        }

        public List<Component> getPublishers() {
            return publishers;
        }

        public List<Component> getConsumers() {
            return consumers;
        }

        @Override
        public int compareTo(DevChannelInfo other) {
            // publisher connectors last
            long publisherConnectors = publishers.stream().filter(Component::isConnector).count();
            long otherPublisherConnectors = other.publishers.stream().filter(Component::isConnector).count();
            if (publisherConnectors != otherPublisherConnectors) {
                return Long.compare(otherPublisherConnectors, publisherConnectors);
            }
            // consumer connectors last
            long consumerConnectors = consumers.stream().filter(Component::isConnector).count();
            long otherConsumersConnectors = other.consumers.stream().filter(Component::isConnector).count();
            if (consumerConnectors != otherConsumersConnectors) {
                return Long.compare(otherConsumersConnectors, consumerConnectors);
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
