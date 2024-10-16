package io.quarkus.smallrye.reactivemessaging.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ChannelBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ChannelDirection;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ConnectorBuildItem;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;

public class WiringHelper {

    private WiringHelper() {
        // Avoid direct instantiation
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

    static void produceIncomingChannel(BuildProducer<ChannelBuildItem> producer, String name) {
        String channelName = normalizeChannelName(name);

        Optional<String> managingConnector = getManagingConnector(ChannelDirection.INCOMING, channelName);
        if (managingConnector.isPresent()) {
            if (isChannelEnabled(ChannelDirection.INCOMING, channelName)) {
                producer.produce(ChannelBuildItem.incoming(channelName, managingConnector.get()));
            }
        } else {
            producer.produce(ChannelBuildItem.incoming(channelName, null));
        }
    }

    static void produceOutgoingChannel(BuildProducer<ChannelBuildItem> producer, String name) {
        String channelName = normalizeChannelName(name);

        Optional<String> managingConnector = getManagingConnector(ChannelDirection.OUTGOING, channelName);
        if (managingConnector.isPresent()) {
            if (isChannelEnabled(ChannelDirection.OUTGOING, channelName)) {
                producer.produce(ChannelBuildItem.outgoing(channelName, managingConnector.get()));
            }
        } else {
            producer.produce(ChannelBuildItem.outgoing(channelName, null));
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
    static Optional<String> getManagingConnector(ChannelDirection direction, String channel) {
        return ConfigProvider.getConfig().getOptionalValue(
                "mp.messaging." + direction.name().toLowerCase() + "." + normalizeChannelName(channel) + ".connector",
                String.class);
    }

    /**
     * Checks if the given channel is enabled / disabled in the configuration
     *
     * @param direction the direction (incoming or outgoing)
     * @param channel the channel name
     * @return {@code true} if the channel is enabled, {@code false} otherwise
     */
    static boolean isChannelEnabled(ChannelDirection direction, String channel) {
        return ConfigProvider.getConfig()
                .getOptionalValue(
                        "mp.messaging." + direction.name().toLowerCase() + "." + normalizeChannelName(channel) + ".enabled",
                        Boolean.class)
                .orElse(true);
    }

    /**
     * Checks if the given class is an inbound (incoming) connector.
     *
     * @param ci the class
     * @return {@code true} if the class implements the inbound connector interface
     */
    static boolean isInboundConnector(ClassInfo ci) {
        return ci.interfaceNames().contains(ReactiveMessagingDotNames.INCOMING_CONNECTOR_FACTORY)
                || ci.interfaceNames().contains(ReactiveMessagingDotNames.INBOUND_CONNECTOR);
    }

    /**
     * Checks if the given class is an outbound (outgoing) connector.
     *
     * @param ci the class
     * @return {@code true} if the class implements the outbound connector interface
     */
    static boolean isOutboundConnector(ClassInfo ci) {
        return ci.interfaceNames().contains(ReactiveMessagingDotNames.OUTGOING_CONNECTOR_FACTORY)
                || ci.interfaceNames().contains(ReactiveMessagingDotNames.OUTBOUND_CONNECTOR);
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
                .declaredAnnotationsWithRepeatable(ReactiveMessagingDotNames.CONNECTOR_ATTRIBUTES, index.getIndex())
                .stream().flatMap(ai -> Arrays.stream(ai.value().asNestedArray())).collect(Collectors.toList());
        if (attributes.isEmpty()) {
            AnnotationInstance attribute = bi.getImplClazz().declaredAnnotation(ReactiveMessagingDotNames.CONNECTOR_ATTRIBUTE);
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
     * Normalize the name of a given channel.
     *
     * Concatenate the channel name with double quotes when it contains dots.
     * <p>
     * Otherwise, the SmallRye Reactive Messaging only considers the
     * text up to the first occurrence of a dot as the channel name.
     *
     * @param name the channel name.
     * @return normalized channel name.
     */
    private static String normalizeChannelName(String name) {
        return name != null && !name.startsWith("\"") && name.contains(".") ? "\"" + name + "\"" : name;
    }

    /**
     * Finds a connector by name and direction in the given list.
     *
     * @param connectors the list of connectors
     * @param name the name
     * @param direction the direction
     * @return the found connector, {@code null} otherwise
     */
    static ConnectorBuildItem find(List<ConnectorBuildItem> connectors, String name, ChannelDirection direction) {
        for (ConnectorBuildItem connector : connectors) {
            if (connector.getDirection() == direction && connector.getName().equalsIgnoreCase(name)) {
                return connector;
            }
        }
        return null;
    }

    static boolean hasConnector(List<ConnectorBuildItem> connectors, ChannelDirection direction, String name) {
        return connectors.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name) && c.getDirection() == direction);
    }

    static Class<?> toType(String type) throws ClassNotFoundException {
        switch (type.toLowerCase()) {
            case "boolean":
                return Boolean.class;
            case "int":
                return Integer.class;
            case "string":
                return String.class;
            case "double":
                return Double.class;
            case "float":
                return Float.class;
            case "short":
                return Short.class;
            case "long":
                return Long.class;
            case "byte":
                return Byte.class;
            default:
                return WiringHelper.class.getClassLoader().loadClass(type);
        }
    }

    static boolean isSynthetic(MethodInfo method) {
        short flag = method.flags();
        return (flag & Opcodes.ACC_SYNTHETIC) != 0;
    }

    static Optional<AnnotationInstance> getAnnotation(TransformedAnnotationsBuildItem transformedAnnotations,
            InjectionPointInfo injectionPoint,
            DotName annotationName) {
        // For field IP -> set of field annotations
        // For method param IP -> set of param annotations
        Collection<AnnotationInstance> annotations = transformedAnnotations
                .getAnnotations(injectionPoint.getAnnotationTarget());
        for (AnnotationInstance annotation : annotations) {
            if (annotationName.equals(annotation.name())) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }
}
