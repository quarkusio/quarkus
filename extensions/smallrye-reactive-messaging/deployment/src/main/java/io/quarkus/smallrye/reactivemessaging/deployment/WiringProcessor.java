package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.*;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.find;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.getConnectorAttributes;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.getConnectorName;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.isEmitter;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.isInboundConnector;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.isOutboundConnector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.smallrye.reactive.messaging.wiring.WiringException;

public class WiringProcessor {

    private static final Logger LOGGER = Logger
            .getLogger("io.quarkus.smallrye-reactive-messaging.deployment.processor");

    @BuildStep
    void discoverConnectors(BeanDiscoveryFinishedBuildItem beans, CombinedIndexBuildItem index,
            BuildProducer<ConnectorBuildItem> builder) {
        beans.geBeans().stream()
                .filter(bi -> bi.getQualifier(ReactiveMessagingDotNames.CONNECTOR).isPresent())
                .forEach(bi -> {
                    if (isInboundConnector(bi.getImplClazz())) {
                        builder.produce(
                                ConnectorBuildItem.createInboundConnector(getConnectorName(bi),
                                        getConnectorAttributes(bi, index,
                                                ConnectorAttribute.Direction.INCOMING,
                                                ConnectorAttribute.Direction.INCOMING_AND_OUTGOING)));
                    }
                    if (isOutboundConnector(bi.getImplClazz())) {
                        builder.produce(
                                ConnectorBuildItem.createOutboundConnector(getConnectorName(bi),
                                        getConnectorAttributes(bi, index,
                                                ConnectorAttribute.Direction.OUTGOING,
                                                ConnectorAttribute.Direction.INCOMING_AND_OUTGOING)));
                    }
                });
    }

    @BuildStep
    public void collectAllApplicationChannels(BuildProducer<IncomingChannelBuildItem> incomings,
            BuildProducer<OutgoingChannelBuildItem> outgoings,
            CombinedIndexBuildItem index) {

        Collection<AnnotationInstance> annotations = index.getIndex().getAnnotations(ReactiveMessagingDotNames.INCOMING);
        for (AnnotationInstance annotation : annotations) {
            String name = annotation.value().asString();
            produceIncomingChannel(incomings, name);
        }

        annotations = index.getIndex().getAnnotations(ReactiveMessagingDotNames.INCOMINGS);
        for (AnnotationInstance annotation : annotations) {
            for (AnnotationValue channel : annotation.values()) {
                String name = channel.asString();
                produceIncomingChannel(incomings, name);
            }
        }
        annotations = index.getIndex().getAnnotations(ReactiveMessagingDotNames.OUTGOING);
        for (AnnotationInstance annotation : annotations) {
            String name = annotation.value().asString();
            produceOutgoingChannel(outgoings, name);
        }

        annotations = index.getIndex().getAnnotations(ReactiveMessagingDotNames.CHANNEL);
        for (AnnotationInstance annotation : annotations) {
            String name = annotation.value().asString();
            if (!name.isBlank()) {
                if (isEmitter(annotation)) {
                    produceOutgoingChannel(outgoings, name);
                } else {
                    produceIncomingChannel(incomings, name);
                }
            }
        }
    }

    @BuildStep
    public void detectOrphanChannels(List<IncomingChannelBuildItem> incomings,
            List<OutgoingChannelBuildItem> outgoings,
            BuildProducer<OrphanChannelBuildItem> builder) {
        Map<String, IncomingChannelBuildItem> inc = new HashMap<>();
        Map<String, OutgoingChannelBuildItem> out = new HashMap<>();
        for (IncomingChannelBuildItem incoming : incomings) {
            inc.put(incoming.getName(), incoming);
        }
        for (OutgoingChannelBuildItem outgoing : outgoings) {
            out.put(outgoing.getName(), outgoing);
        }

        Set<String> orphanInboundChannels = new HashSet<>(inc.keySet());
        Set<String> orphanOutboundChannels = new HashSet<>(out.keySet());

        // Orphan inbounds: all inbounds that do not have a matching outbound
        orphanInboundChannels.removeAll(out.keySet());

        // Orphan outbounds: all outbounds that do not have a matching inbound
        orphanOutboundChannels.removeAll(inc.keySet());

        // We need to remove all channels that are managed by connectors
        for (String channel : orphanInboundChannels) {
            if (!inc.get(channel).isManagedByAConnector()) {
                builder.produce(OrphanChannelBuildItem.of(ConnectorBuildItem.Direction.INBOUND, channel));
            }
        }
        for (String channel : orphanOutboundChannels) {
            if (!out.get(channel).isManagedByAConnector()) {
                builder.produce(OrphanChannelBuildItem.of(ConnectorBuildItem.Direction.OUTBOUND, channel));
            }
        }
    }

    @BuildStep
    void generateDocumentationItem(BuildProducer<ConfigDescriptionBuildItem> config,
            List<ConnectorManagedChannelBuildItem> channels, List<ConnectorBuildItem> connectors)
            throws ClassNotFoundException {
        for (ConnectorManagedChannelBuildItem channel : channels) {
            ConnectorBuildItem connector = find(connectors, channel.getConnector(), channel.getDirection());
            String prefix = "mp.messaging."
                    + (channel.getDirection() == ConnectorBuildItem.Direction.INBOUND ? "incoming" : "outgoing") + "."
                    + channel.getName() + ".";
            if (connector != null) {
                for (ConnectorAttribute attribute : connector.getAttributes()) {
                    ConfigDescriptionBuildItem cfg = new ConfigDescriptionBuildItem(prefix + attribute.name(),
                            toType(attribute.type()),
                            attribute.defaultValue().equalsIgnoreCase(ConnectorAttribute.NO_VALUE) ? null
                                    : attribute.defaultValue(),
                            attribute.description(), attribute.type(), Collections.emptyList(), ConfigPhase.RUN_TIME);
                    config.produce(cfg);
                }
            }
        }
    }

    private Class<?> toType(String type) throws ClassNotFoundException {
        if (type.equalsIgnoreCase("string")) {
            return String.class;
        }
        if (type.equalsIgnoreCase("int")) {
            return Integer.class;
        }
        if (type.equalsIgnoreCase("long")) {
            return Long.class;
        }
        if (type.equalsIgnoreCase("boolean")) {
            return Boolean.class;
        }
        return this.getClass().getClassLoader().loadClass(type);
    }

    @BuildStep
    public void autoConfigureConnectorForOrphansAndProduceManagedChannels(
            ReactiveMessagingBuildTimeConfig buildTimeConfig,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            BuildProducer<ConnectorManagedChannelBuildItem> channels,
            List<OrphanChannelBuildItem> orphans, List<ConnectorBuildItem> connectors,
            List<IncomingChannelBuildItem> incomings, List<OutgoingChannelBuildItem> outgoings,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        // For each orphan, if we have a single matching connector - add the .connector attribute to the config
        Set<String> incomingConnectors = new HashSet<>();
        Set<String> outgoingConnectors = new HashSet<>();
        for (ConnectorBuildItem connector : connectors) {
            if (connector.getDirection() == ConnectorBuildItem.Direction.INBOUND) {
                incomingConnectors.add(connector.getName());
            } else {
                outgoingConnectors.add(connector.getName());
            }
        }

        if (incomingConnectors.size() == 1 && buildTimeConfig.autoConnectorAttachment) {
            String connector = incomingConnectors.iterator().next();
            // Single incoming connector, set mp.messaging.incoming.orphan-channel.connector
            for (OrphanChannelBuildItem orphan : orphans) {
                if (orphan.getDirection() == ConnectorBuildItem.Direction.INBOUND) {
                    config.produce(new RunTimeConfigurationDefaultBuildItem(
                            "mp.messaging.incoming." + orphan.getName() + ".connector", connector));
                    LOGGER.infof("Configuring the channel '%s' to be managed by the connector '%s'", orphan.getName(),
                            connector);
                    channels.produce(new ConnectorManagedChannelBuildItem(orphan.getName(),
                            ConnectorBuildItem.Direction.INBOUND, connector));
                }
            }
        }

        if (outgoingConnectors.size() == 1 && buildTimeConfig.autoConnectorAttachment) {
            String connector = outgoingConnectors.iterator().next();
            // Single outgoing connector, set mp.messaging.outgoing.orphan-channel.connector
            for (OrphanChannelBuildItem orphan : orphans) {
                if (orphan.getDirection() == ConnectorBuildItem.Direction.OUTBOUND) {
                    config.produce(new RunTimeConfigurationDefaultBuildItem(
                            "mp.messaging.outgoing." + orphan.getName() + ".connector", connector));
                    LOGGER.infof("Configuring the channel '%s' to be managed by the connector '%s'", orphan.getName(),
                            connector);
                    channels.produce(new ConnectorManagedChannelBuildItem(orphan.getName(),
                            ConnectorBuildItem.Direction.OUTBOUND, connector));
                }
            }
        }

        // Now iterate over the configured channels.
        for (IncomingChannelBuildItem incoming : incomings) {
            if (incoming.isManagedByAConnector()) {
                if (!hasConnector(connectors, ConnectorBuildItem.Direction.INBOUND, incoming.getConnector())) {
                    errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                            new WiringException("The channel '" + incoming.getName()
                                    + "' is configured with an unknown connector (" + incoming.getConnector() + ")")));
                } else {
                    channels.produce(
                            new ConnectorManagedChannelBuildItem(incoming.getName(), ConnectorBuildItem.Direction.INBOUND,
                                    incoming.getConnector()));
                }
            }
        }

        for (OutgoingChannelBuildItem outgoing : outgoings) {
            if (outgoing.isManagedByAConnector()) {
                if (!hasConnector(connectors, ConnectorBuildItem.Direction.OUTBOUND, outgoing.getConnector())) {
                    errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                            new WiringException("The channel '" + outgoing.getName()
                                    + "' is configured with an unknown connector (" + outgoing.getConnector() + ")")));
                } else {
                    channels.produce(
                            new ConnectorManagedChannelBuildItem(outgoing.getName(), ConnectorBuildItem.Direction.OUTBOUND,
                                    outgoing.getConnector()));
                }
            }
        }

    }

}
