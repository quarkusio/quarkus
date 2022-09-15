package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.BLOCKING;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.find;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.getConnectorAttributes;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.getConnectorName;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.hasConnector;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.isInboundConnector;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.isOutboundConnector;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.produceIncomingChannel;
import static io.quarkus.smallrye.reactivemessaging.deployment.WiringHelper.produceOutgoingChannel;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ChannelBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ChannelDirection;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ConnectorBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ConnectorManagedChannelBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.InjectedChannelBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.InjectedEmitterBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.MediatorBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.OrphanChannelBuildItem;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.smallrye.reactive.messaging.providers.wiring.WiringException;

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
                                ConnectorBuildItem.createIncomingConnector(getConnectorName(bi),
                                        getConnectorAttributes(bi, index,
                                                ConnectorAttribute.Direction.INCOMING,
                                                ConnectorAttribute.Direction.INCOMING_AND_OUTGOING)));
                    }
                    if (isOutboundConnector(bi.getImplClazz())) {
                        builder.produce(
                                ConnectorBuildItem.createOutgoingConnector(getConnectorName(bi),
                                        getConnectorAttributes(bi, index,
                                                ConnectorAttribute.Direction.OUTGOING,
                                                ConnectorAttribute.Direction.INCOMING_AND_OUTGOING)));
                    }
                });
    }

    @BuildStep
    void extractComponents(BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<ChannelBuildItem> appChannels,
            BuildProducer<MediatorBuildItem> mediatorMethods,
            BuildProducer<InjectedEmitterBuildItem> emitters,
            BuildProducer<InjectedChannelBuildItem> channels,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            BuildProducer<ConfigDescriptionBuildItem> configDescriptionBuildItemBuildProducer) {

        Map<String, AnnotationInstance> emitterFactories = new HashMap<>();
        // We need to collect all business methods annotated with @Incoming/@Outgoing first
        for (BeanInfo bean : beanDiscoveryFinished.beanStream().classBeans()) {
            // TODO: add support for inherited business methods
            //noinspection OptionalGetWithoutIsPresent
            AnnotationInstance emitterFactory = transformedAnnotations.getAnnotation(bean.getTarget().get(),
                    ReactiveMessagingDotNames.EMITTER_FACTORY_FOR);
            if (emitterFactory != null) {
                emitterFactories.put(emitterFactory.value().asClass().name().toString(), emitterFactory);
            }

            for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                // @Incoming is repeatable
                AnnotationInstance incoming = transformedAnnotations.getAnnotation(method,
                        ReactiveMessagingDotNames.INCOMING);
                AnnotationInstance incomings = transformedAnnotations.getAnnotation(method,
                        ReactiveMessagingDotNames.INCOMINGS);
                AnnotationInstance outgoing = transformedAnnotations.getAnnotation(method,
                        ReactiveMessagingDotNames.OUTGOING);
                AnnotationInstance blocking = transformedAnnotations.getAnnotation(method,
                        BLOCKING);
                if (incoming != null || incomings != null || outgoing != null) {
                    handleMethodAnnotatedWithIncoming(appChannels, validationErrors, configDescriptionBuildItemBuildProducer,
                            method, incoming);
                    handleMethodAnnotationWithIncomings(appChannels, validationErrors, configDescriptionBuildItemBuildProducer,
                            method, incomings);
                    handleMethodAnnotationWithOutgoing(appChannels, validationErrors, configDescriptionBuildItemBuildProducer,
                            method, outgoing);

                    if (WiringHelper.isSynthetic(method)) {
                        continue;
                    }

                    mediatorMethods.produce(new MediatorBuildItem(bean, method));
                    LOGGER.debugf("Found mediator business method %s declared on %s", method, bean);
                } else if (blocking != null) {
                    validationErrors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                            new DeploymentException(
                                    "@Blocking used on " + method + " which has no @Incoming or @Outgoing annotation")));
                }
            }
        }

        for (InjectionPointInfo injectionPoint : beanDiscoveryFinished.getInjectionPoints()) {
            Optional<AnnotationInstance> broadcast = WiringHelper.getAnnotation(transformedAnnotations, injectionPoint,
                    ReactiveMessagingDotNames.BROADCAST);
            Optional<AnnotationInstance> channel = WiringHelper.getAnnotation(transformedAnnotations, injectionPoint,
                    ReactiveMessagingDotNames.CHANNEL);
            Optional<AnnotationInstance> legacyChannel = WiringHelper.getAnnotation(transformedAnnotations, injectionPoint,
                    ReactiveMessagingDotNames.LEGACY_CHANNEL);

            String injectionType = injectionPoint.getRequiredType().name().toString();
            AnnotationInstance emitterType = emitterFactories.get(injectionType);

            boolean isLegacyEmitter = injectionPoint.getRequiredType().name()
                    .equals(ReactiveMessagingDotNames.LEGACY_EMITTER);

            if (emitterType != null) {
                if (isLegacyEmitter) {
                    // Deprecated Emitter from SmallRye (emitter, channel and on overflow have been added to the spec)
                    handleEmitter(transformedAnnotations, appChannels, emitters, validationErrors, injectionPoint,
                            emitterType, broadcast, legacyChannel, ReactiveMessagingDotNames.LEGACY_ON_OVERFLOW);
                } else {
                    // New emitter from the spec, or Mutiny emitter
                    handleEmitter(transformedAnnotations, appChannels, emitters, validationErrors, injectionPoint,
                            emitterType, broadcast, channel, ReactiveMessagingDotNames.ON_OVERFLOW);
                }
            } else {
                if (channel.isPresent()) {
                    handleChannelInjection(appChannels, channels, channel.get());
                }

                if (legacyChannel.isPresent()) {
                    handleChannelInjection(appChannels, channels, legacyChannel.get());
                }
            }

        }

    }

    private void handleChannelInjection(BuildProducer<ChannelBuildItem> appChannels,
            BuildProducer<InjectedChannelBuildItem> channels,
            AnnotationInstance channel) {
        String name = channel.value().asString();
        if (name != null && !name.trim().isEmpty()) {
            produceIncomingChannel(appChannels, name);
            channels.produce(InjectedChannelBuildItem.of(name));
        }
    }

    private void handleEmitter(TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<ChannelBuildItem> appChannels,
            BuildProducer<InjectedEmitterBuildItem> emitters,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            InjectionPointInfo injectionPoint,
            AnnotationInstance emitterType,
            Optional<AnnotationInstance> broadcast,
            Optional<AnnotationInstance> annotation,
            DotName onOverflowAnnotation) {
        if (annotation.isEmpty()) {
            validationErrors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    new DeploymentException(
                            "Invalid emitter injection - @Channel is required for " + injectionPoint
                                    .getTargetInfo())));
        } else {
            String channelName = annotation.get().value().asString();
            Optional<AnnotationInstance> overflow = WiringHelper.getAnnotation(transformedAnnotations, injectionPoint,
                    onOverflowAnnotation);
            createEmitter(appChannels, emitters, injectionPoint, channelName, emitterType, overflow, broadcast);
        }
    }

    private void handleMethodAnnotationWithOutgoing(BuildProducer<ChannelBuildItem> appChannels,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            BuildProducer<ConfigDescriptionBuildItem> configDescriptionBuildItemBuildProducer,
            MethodInfo method, AnnotationInstance outgoing) {
        if (outgoing != null && outgoing.value().asString().isEmpty()) {
            validationErrors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    new DeploymentException("Empty @Outgoing annotation on method " + method)));
        }
        if (outgoing != null) {
            configDescriptionBuildItemBuildProducer.produce(new ConfigDescriptionBuildItem(
                    "mp.messaging.outgoing." + outgoing.value().asString() + ".connector", null,
                    "The connector to use", null, null, ConfigPhase.BUILD_TIME));

            produceOutgoingChannel(appChannels, outgoing.value().asString());
        }
    }

    private void handleMethodAnnotationWithIncomings(BuildProducer<ChannelBuildItem> appChannels,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            BuildProducer<ConfigDescriptionBuildItem> configDescriptionBuildItemBuildProducer,
            MethodInfo method, AnnotationInstance incomings) {
        if (incomings != null) {
            for (AnnotationInstance instance : incomings.value().asNestedArray()) {
                if (instance.value().asString().isEmpty()) {
                    validationErrors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                            new DeploymentException("Empty @Incoming annotation on method " + method)));
                }
                configDescriptionBuildItemBuildProducer.produce(new ConfigDescriptionBuildItem(
                        "mp.messaging.incoming." + instance.value().asString() + ".connector", null,
                        "The connector to use", null, null, ConfigPhase.BUILD_TIME));
                produceIncomingChannel(appChannels, instance.value().asString());
            }
        }
    }

    private void handleMethodAnnotatedWithIncoming(BuildProducer<ChannelBuildItem> appChannels,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            BuildProducer<ConfigDescriptionBuildItem> configDescriptionBuildItemBuildProducer,
            MethodInfo method, AnnotationInstance incoming) {
        if (incoming != null && incoming.value().asString().isEmpty()) {
            validationErrors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    new DeploymentException("Empty @Incoming annotation on method " + method)));
        }
        if (incoming != null) {
            configDescriptionBuildItemBuildProducer.produce(new ConfigDescriptionBuildItem(
                    "mp.messaging.incoming." + incoming.value().asString() + ".connector", null,
                    "The connector to use", null, null, ConfigPhase.BUILD_TIME));
            produceIncomingChannel(appChannels, incoming.value().asString());
        }
    }

    @BuildStep
    public void detectOrphanChannels(List<ChannelBuildItem> channels,
            BuildProducer<OrphanChannelBuildItem> builder) {
        Map<String, ChannelBuildItem> inc = new HashMap<>();
        Map<String, ChannelBuildItem> out = new HashMap<>();
        for (ChannelBuildItem channel : channels) {
            if (channel.getDirection() == ChannelDirection.INCOMING) {
                inc.put(channel.getName(), channel);
            } else {
                out.put(channel.getName(), channel);
            }
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
                builder.produce(OrphanChannelBuildItem.of(ChannelDirection.INCOMING, channel));
            }
        }
        for (String channel : orphanOutboundChannels) {
            if (!out.get(channel).isManagedByAConnector()) {
                builder.produce(OrphanChannelBuildItem.of(ChannelDirection.OUTGOING, channel));
            }
        }
    }

    @BuildStep
    void generateDocumentationItem(BuildProducer<ConfigDescriptionBuildItem> config,
            List<ConnectorManagedChannelBuildItem> channels, List<ConnectorBuildItem> connectors) {
        Parser markdownParser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        for (ConnectorManagedChannelBuildItem channel : channels) {
            ConnectorBuildItem connector = find(connectors, channel.getConnector(), channel.getDirection());
            String prefix = "mp.messaging."
                    + (channel.getDirection() == ChannelDirection.INCOMING ? "incoming" : "outgoing") + "."
                    + channel.getName() + ".";
            if (connector != null) {
                for (ConnectorAttribute attribute : connector.getAttributes()) {
                    ConfigDescriptionBuildItem cfg = new ConfigDescriptionBuildItem(
                            prefix + attribute.name(),
                            attribute.defaultValue().equalsIgnoreCase(ConnectorAttribute.NO_VALUE) ? null
                                    : attribute.defaultValue(),
                            renderer.render(markdownParser.parse(attribute.description())),
                            attribute.type(), Collections.emptyList(), ConfigPhase.RUN_TIME);
                    config.produce(cfg);
                }
            }
        }
    }

    @BuildStep
    public void autoConfigureConnectorForOrphansAndProduceManagedChannels(
            ReactiveMessagingBuildTimeConfig buildTimeConfig,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            BuildProducer<ConnectorManagedChannelBuildItem> connectorManagedChannels,
            List<OrphanChannelBuildItem> orphans, List<ConnectorBuildItem> connectors,
            List<ChannelBuildItem> channels,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        // For each orphan, if we have a single matching connector - add the .connector attribute to the config
        Set<String> incomingConnectors = new HashSet<>();
        Set<String> outgoingConnectors = new HashSet<>();
        for (ConnectorBuildItem connector : connectors) {
            if (connector.getDirection() == ChannelDirection.INCOMING) {
                incomingConnectors.add(connector.getName());
            } else {
                outgoingConnectors.add(connector.getName());
            }
        }

        if (incomingConnectors.size() == 1 && buildTimeConfig.autoConnectorAttachment) {
            String connector = incomingConnectors.iterator().next();
            // Single incoming connector, set mp.messaging.incoming.orphan-channel.connector
            for (OrphanChannelBuildItem orphan : orphans) {
                if (orphan.getDirection() == ChannelDirection.INCOMING) {
                    config.produce(new RunTimeConfigurationDefaultBuildItem(
                            "mp.messaging.incoming." + orphan.getName() + ".connector", connector));
                    LOGGER.infof("Configuring the channel '%s' to be managed by the connector '%s'", orphan.getName(),
                            connector);
                    connectorManagedChannels.produce(new ConnectorManagedChannelBuildItem(orphan.getName(),
                            ChannelDirection.INCOMING, connector));
                }
            }
        }

        if (outgoingConnectors.size() == 1 && buildTimeConfig.autoConnectorAttachment) {
            String connector = outgoingConnectors.iterator().next();
            // Single outgoing connector, set mp.messaging.outgoing.orphan-channel.connector
            for (OrphanChannelBuildItem orphan : orphans) {
                if (orphan.getDirection() == ChannelDirection.OUTGOING) {
                    config.produce(new RunTimeConfigurationDefaultBuildItem(
                            "mp.messaging.outgoing." + orphan.getName() + ".connector", connector));
                    LOGGER.infof("Configuring the channel '%s' to be managed by the connector '%s'", orphan.getName(),
                            connector);
                    connectorManagedChannels.produce(new ConnectorManagedChannelBuildItem(orphan.getName(),
                            ChannelDirection.OUTGOING, connector));
                }
            }
        }

        // Now iterate over the configured channels.
        for (ChannelBuildItem channel : channels) {
            if (channel.isManagedByAConnector()) {
                if (!hasConnector(connectors, channel.getDirection(), channel.getConnector())) {
                    errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                            new WiringException("The channel '" + channel.getName()
                                    + "' is configured with an unknown connector (" + channel.getConnector() + ")")));
                } else {
                    connectorManagedChannels.produce(new ConnectorManagedChannelBuildItem(channel.getName(),
                            channel.getDirection(), channel.getConnector()));
                }
            }
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void createEmitter(BuildProducer<ChannelBuildItem> appChannels,
            BuildProducer<InjectedEmitterBuildItem> emitters,
            InjectionPointInfo injectionPoint,
            String channelName,
            AnnotationInstance emitter,
            Optional<AnnotationInstance> overflow,
            Optional<AnnotationInstance> broadcast) {
        LOGGER.debugf("Emitter injection point '%s' detected, channel name: '%s'",
                injectionPoint.getTargetInfo(), channelName);

        String emitterTypeName = emitter.value().asClass().name().toString();

        boolean hasBroadcast = false;
        int awaitSubscribers = -1;
        int bufferSize = -1;
        String strategy = null;
        if (broadcast.isPresent()) {
            hasBroadcast = true;
            AnnotationValue value = broadcast.get().value();
            awaitSubscribers = value == null ? 0 : value.asInt();
        }

        if (overflow.isPresent()) {
            AnnotationInstance annotation = overflow.get();
            AnnotationValue maybeBufferSize = annotation.value("bufferSize");
            bufferSize = maybeBufferSize == null ? 0 : maybeBufferSize.asInt();
            strategy = annotation.value().asString();
        }

        produceOutgoingChannel(appChannels, channelName);
        emitters.produce(
                InjectedEmitterBuildItem
                        .of(channelName, emitterTypeName, strategy, bufferSize, hasBroadcast, awaitSubscribers));
    }

}
