package io.quarkus.smallrye.reactivemessaging.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodParameterInfo;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;

public class WiringHelper {

    private WiringHelper() {
        // Avoid direct instantiation
    }

    /**
     * Checks if the given set of annotation contains the {@code Connector} qualifier.
     * 
     * @param qualifiers the set
     * @return {@code true} if the set contains {@code Connector}, {@code false} otherwise.
     */
    static boolean isConnector(Set<AnnotationInstance> qualifiers) {
        return qualifiers.stream().anyMatch(ai -> ai.name().equals(ReactiveMessagingDotNames.CONNECTOR));
    }

    /**
     * Retrieves the connector name from the given bean.
     * Throws a {@link NoSuchElementException} if the given bean does not have the {@code @Connector} qualifier
     * 
     * @param bi the bean
     * @return the connector name
     * @throws NoSuchElementException if the bean does not have the {@code @Connector} qualifier
     */
    static String getConnectorName(BeanInfo bi) {
        return bi.getQualifier(ReactiveMessagingDotNames.CONNECTOR)
                .orElseThrow(() -> new NoSuchElementException("Expecting a @Connector"))
                .value().asString();
    }

    static void produceIncomingChannel(BuildProducer<IncomingChannelBuildItem> producer, String name) {
        Optional<String> managingConnector = getManagingConnector("incoming", name);
        if (managingConnector.isPresent()) {
            if (isChannelEnabled("incoming", name)) {
                producer.produce(IncomingChannelBuildItem.of(name, managingConnector.get()));
            }
        } else {
            producer.produce(IncomingChannelBuildItem.of(name, null));
        }
    }

    static void produceOutgoingChannel(BuildProducer<OutgoingChannelBuildItem> producer, String name) {
        Optional<String> managingConnector = getManagingConnector("outgoing", name);
        if (managingConnector.isPresent()) {
            if (isChannelEnabled("outgoing", name)) {
                producer.produce(OutgoingChannelBuildItem.of(name, managingConnector.get()));
            }
        } else {
            producer.produce(OutgoingChannelBuildItem.of(name, null));
        }
    }

    /**
     * Gets the name of the connector managing the channel if any.
     * This method looks inside the application configuration.
     * 
     * @param direction the direction (incoming or outgoing)
     * @param channel the channel name
     * @return an optional with the connector name if the channel is managed, empty otherwise
     */
    static Optional<String> getManagingConnector(String direction, String channel) {
        return ConfigProvider.getConfig().getOptionalValue("mp.messaging." + direction + "." + channel + ".connector",
                String.class);
    }

    /**
     * Checks if the given channel is enabled / disabled in the configuration
     *
     * @param direction the direction (incoming or outgoing)
     * @param channel the channel name
     * @return {@code true} if the channel is enabled, {@code false} otherwise
     */
    static boolean isChannelEnabled(String direction, String channel) {
        return ConfigProvider.getConfig().getOptionalValue("mp.messaging." + direction + "." + channel + ".enabled",
                Boolean.class).orElse(true);
    }

    /**
     * Checks if the given class is an inbound (incoming) connector.
     * 
     * @param ci the class
     * @return {@code true} if the class implements the inbound connector interface
     */
    static boolean isInboundConnector(ClassInfo ci) {
        // TODO Add the internal interface support
        return ci.interfaceNames().contains(ReactiveMessagingDotNames.INCOMING_CONNECTOR_FACTORY);
    }

    /**
     * Checks if the given class is an outbound (outgoing) connector.
     * 
     * @param ci the class
     * @return {@code true} if the class implements the outbound connector interface
     */
    static boolean isOutboundConnector(ClassInfo ci) {
        // TODO Add the internal interface support
        return ci.interfaceNames().contains(ReactiveMessagingDotNames.OUTGOING_CONNECTOR_FACTORY);
    }

    /**
     * Collects connector attributes from the given connector implementation.
     * 
     * @param bi the bean implementing the connector interfaces
     * @param index the index
     * @param directions the attribute direction to includes in the result
     * @return the list of connector attributes, empty if none
     */
    static List<ConnectorAttribute> getConnectorAttributes(BeanInfo bi, CombinedIndexBuildItem index,
            ConnectorAttribute.Direction... directions) {
        List<AnnotationInstance> attributes = bi.getImplClazz()
                .classAnnotationsWithRepeatable(ReactiveMessagingDotNames.CONNECTOR_ATTRIBUTES, index.getIndex())
                .stream().flatMap(ai -> Arrays.stream(ai.value().asNestedArray())).collect(Collectors.toList());
        if (attributes.isEmpty()) {
            AnnotationInstance attribute = bi.getImplClazz().classAnnotation(ReactiveMessagingDotNames.CONNECTOR_ATTRIBUTE);
            if (attribute != null) {
                attributes = Collections.singletonList(attribute);
            }
        }

        List<ConnectorAttribute> att = new ArrayList<>();
        for (AnnotationInstance instance : attributes) {
            ConnectorAttribute.Direction direction = ConnectorAttribute.Direction
                    .valueOf(instance.value("direction").asString().toUpperCase());

            if (Arrays.asList(directions).contains(direction)) {
                ConnectorAttribute literal = createConnectorAttribute(instance, direction);
                att.add(literal);
            }
        }
        return att;
    }

    /**
     * Creates a {@code ConnectorAttribute} literal for the given instance.
     * 
     * @param instance the instance
     * @param direction the direction
     * @return the connector attribute.
     */
    private static ConnectorAttribute createConnectorAttribute(AnnotationInstance instance,
            ConnectorAttribute.Direction direction) {
        String name = instance.value("name").asString();
        String type = instance.value("type").asString();
        String description = instance.value("description").asString();

        boolean hidden = getBooleanValueOrDefault(instance, "hidden");
        boolean mandatory = getBooleanValueOrDefault(instance, "hidden");
        boolean deprecated = getBooleanValueOrDefault(instance, "deprecated");
        String defaultValue = getStringValueOrDefault(instance, "defaultValue");
        String alias = getStringValueOrDefault(instance, "alias");

        return ConnectorAttributeLiteral.create(name, description, hidden, mandatory,
                direction, defaultValue, deprecated, alias, type);
    }

    private static String getStringValueOrDefault(AnnotationInstance instance, String attribute) {
        AnnotationValue value = instance.value(attribute);
        if (value != null) {
            return value.asString();
        }
        return ConnectorAttribute.NO_VALUE;
    }

    private static boolean getBooleanValueOrDefault(AnnotationInstance instance, String attribute) {
        AnnotationValue value = instance.value(attribute);
        if (value != null) {
            return value.asBoolean();
        }
        return false;
    }

    /**
     * Checks if the given {@code @Channel} annotation instance is injecting an {@code Emitter} or {@code MutinyEmitter}.
     * 
     * @param instance the {@code @Channel} instance
     * @return {@code true} if the injected type is an emitter, {@code false} otherwise.
     */
    static boolean isEmitter(AnnotationInstance instance) {
        DotName type = null;
        if (instance.target().kind() == AnnotationTarget.Kind.FIELD) {
            type = instance.target().asField().type().name();
        } else if (instance.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
            MethodParameterInfo info = instance.target().asMethodParameter();
            type = info.method().parameters().get(info.position()).name();
        }
        return type != null && (type.equals(ReactiveMessagingDotNames.EMITTER)
                || type.equals(ReactiveMessagingDotNames.MUTINY_EMITTER));
    }

    /**
     * Finds a connector by name and direction in the given list.
     * 
     * @param connectors the list of connectors
     * @param name the name
     * @param direction the direction
     * @return the found connector, {@code null} otherwise
     */
    static ConnectorBuildItem find(List<ConnectorBuildItem> connectors, String name, ConnectorBuildItem.Direction direction) {
        for (ConnectorBuildItem connector : connectors) {
            if (connector.getDirection() == direction && connector.getName().equalsIgnoreCase(name)) {
                return connector;
            }
        }
        return null;
    }

    static boolean hasConnector(List<ConnectorBuildItem> connectors, ConnectorBuildItem.Direction direction, String name) {
        return connectors.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name) && c.getDirection() == direction);
    }
}
