package io.quarkus.smallrye.reactivemessaging.runtime.produi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusMediatorConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder.SmallRyeReactiveMessagingContext;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.providers.extension.ChannelConfiguration;

/**
 * Read-only Prod UI view of the SmallRye Reactive Messaging channels. It shows
 * each channel with its publishers and subscribers (emitters, injected channels,
 * mediators and connectors), mirroring the Dev UI Channels page and reusing its
 * web component. Unlike the Dev UI helper it lives in the always-present runtime
 * module and, for connectors, exposes only the connector name - never the
 * connector configuration properties, which may contain credentials.
 */
@ApplicationScoped
public class ReactiveMessagingProdUIService {

    private static final String OUTGOING_PREFIX = "mp.messaging.outgoing.";
    private static final String INCOMING_PREFIX = "mp.messaging.incoming.";
    private static final String CONNECTOR_SUFFIX = ".connector";

    @Inject
    Instance<SmallRyeReactiveMessagingContext> context;

    @Inject
    Config config;

    @NonBlocking
    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only overview of the reactive messaging channels, publishers and subscribers")
    public List<ChannelInfo> getInfo() {
        Map<String, List<ComponentInfo>> publishers = new HashMap<>();
        Map<String, List<ComponentInfo>> consumers = new HashMap<>();
        Function<String, List<ComponentInfo>> fun = e -> new ArrayList<>();

        // Connectors, derived from config. Only the connector name is exposed
        // (never the connector properties, which may hold secrets).
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(OUTGOING_PREFIX) && propertyName.endsWith(CONNECTOR_SUFFIX)) {
                String channel = propertyName.substring(OUTGOING_PREFIX.length(),
                        propertyName.length() - CONNECTOR_SUFFIX.length());
                consumers.computeIfAbsent(channel, fun).add(connector(propertyName));
            } else if (propertyName.startsWith(INCOMING_PREFIX) && propertyName.endsWith(CONNECTOR_SUFFIX)) {
                String channel = propertyName.substring(INCOMING_PREFIX.length(),
                        propertyName.length() - CONNECTOR_SUFFIX.length());
                publishers.computeIfAbsent(channel, fun).add(connector(propertyName));
            }
        }

        if (context.isResolvable()) {
            SmallRyeReactiveMessagingContext ctx = context.get();

            for (EmitterConfiguration emitter : ctx.getEmitterConfigurations()) {
                String description = (emitter.broadcast() ? "<span class=\"annotation\">&#64;Broadcast</span> " : "")
                        + asCode(emitter.name());
                publishers.computeIfAbsent(emitter.name(), fun).add(new ComponentInfo("EMITTER", description));
            }
            for (ChannelConfiguration channel : ctx.getChannelConfigurations()) {
                consumers.computeIfAbsent(channel.channelName, fun)
                        .add(new ComponentInfo("CHANNEL", asCode(channel.channelName)));
            }
            for (QuarkusMediatorConfiguration mediator : ctx.getMediatorConfigurations()) {
                boolean isProcessor = !mediator.getIncoming().isEmpty() && !mediator.getOutgoings().isEmpty();
                if (isProcessor) {
                    for (String outgoing : mediator.getOutgoings()) {
                        publishers.computeIfAbsent(outgoing, fun)
                                .add(new ComponentInfo("PROCESSOR", asMethod(mediator.methodAsString())));
                    }
                    for (String incoming : mediator.getIncoming()) {
                        consumers.computeIfAbsent(incoming, fun)
                                .add(new ComponentInfo("PROCESSOR", asMethod(mediator.methodAsString())));
                    }
                } else if (!mediator.getOutgoings().isEmpty()) {
                    for (String outgoing : mediator.getOutgoings()) {
                        String description = asMethod(mediator.methodAsString())
                                + (mediator.getBroadcast() ? "[broadcast: true]" : "");
                        publishers.computeIfAbsent(outgoing, fun)
                                .add(new ComponentInfo("PUBLISHER", description));
                    }
                } else if (!mediator.getIncoming().isEmpty()) {
                    for (String incoming : mediator.getIncoming()) {
                        consumers.computeIfAbsent(incoming, fun)
                                .add(new ComponentInfo("SUBSCRIBER", asMethod(mediator.methodAsString())));
                    }
                }
            }
        }

        Set<String> channels = new HashSet<>();
        channels.addAll(publishers.keySet());
        channels.addAll(consumers.keySet());

        List<ChannelInfo> infos = new ArrayList<>();
        for (String channel : channels) {
            infos.add(new ChannelInfo(channel,
                    publishers.getOrDefault(channel, List.of()),
                    consumers.getOrDefault(channel, List.of())));
        }
        infos.sort((a, b) -> a.name().compareTo(b.name()));
        return infos;
    }

    private ComponentInfo connector(String connectorPropertyName) {
        String connector = config.getValue(connectorPropertyName, String.class);
        return new ComponentInfo("CONNECTOR", asCode(connector));
    }

    private static String asMethod(String value) {
        return asCode(value + "()");
    }

    private static String asCode(String value) {
        return "<code>" + value + "</code>";
    }

    public record ComponentInfo(String type, String description, boolean isConnector) {
        public ComponentInfo(String type, String description) {
            this(type, description, "CONNECTOR".equals(type));
        }
    }

    public record ChannelInfo(String name, List<ComponentInfo> publishers, List<ComponentInfo> consumers) {
    }
}
