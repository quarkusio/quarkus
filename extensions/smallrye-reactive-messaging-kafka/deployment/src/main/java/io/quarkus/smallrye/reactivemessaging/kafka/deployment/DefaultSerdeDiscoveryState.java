package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.smallrye.reactivemessaging.deployment.items.ChannelDirection;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ConnectorManagedChannelBuildItem;
import io.smallrye.reactive.messaging.kafka.KafkaConnector;

class DefaultSerdeDiscoveryState {
    private final IndexView index;

    private final Map<String, Boolean> isKafkaConnector = new HashMap<>();
    private final Set<String> alreadyConfigured = new HashSet<>();

    private Boolean connectorHasKeySerializer;
    private Boolean connectorHasValueSerializer;
    private Boolean connectorHasKeyDeserializer;
    private Boolean connectorHasValueDeserializer;

    private Boolean hasConfluent;
    private Boolean hasApicurio1;
    private Boolean hasApicurio2;
    private Boolean hasJsonb;

    DefaultSerdeDiscoveryState(IndexView index) {
        this.index = index;
    }

    Config getConfig() {
        return ConfigProvider.getConfig();
    }

    boolean isKafkaConnector(List<ConnectorManagedChannelBuildItem> channelsManagedByConnectors, boolean incoming,
            String channelName) {
        // First look in the channelsManagedByConnectors list
        Optional<ConnectorManagedChannelBuildItem> match = channelsManagedByConnectors.stream().filter(cn -> cn
                .getDirection() == (incoming ? ChannelDirection.INCOMING : ChannelDirection.OUTGOING)
                && cn.getName().equalsIgnoreCase(channelName)).findFirst();
        if (match.isPresent()) {
            return true;
        }

        String channelType = incoming ? "incoming" : "outgoing";
        return isKafkaConnector.computeIfAbsent(channelType + "|" + channelName, ignored -> {
            String connectorKey = "mp.messaging." + channelType + "." + channelName + ".connector";
            String connector = getConfig()
                    .getOptionalValue(connectorKey, String.class)
                    .orElse("ignored");
            return KafkaConnector.CONNECTOR_NAME.equals(connector);
        });
    }

    boolean shouldNotConfigure(String key) {
        // if we know at build time that key/value [de]serializer is configured on the connector,
        // we should NOT emit default configuration for key/value [de]serializer on the channel
        // (in other words, only a user can explicitly override a connector configuration)
        //
        // more config properties could possibly be handled in the same way, but these should suffice for now

        if (key.startsWith("mp.messaging.outgoing.") && key.endsWith(".key.serializer")) {
            if (connectorHasKeySerializer == null) {
                connectorHasKeySerializer = getConfig()
                        .getOptionalValue("mp.messaging.connector." + KafkaConnector.CONNECTOR_NAME + ".key.serializer",
                                String.class)
                        .isPresent();
            }
            return connectorHasKeySerializer;
        }
        if (key.startsWith("mp.messaging.outgoing.") && key.endsWith(".value.serializer")) {
            if (connectorHasValueSerializer == null) {
                connectorHasValueSerializer = getConfig()
                        .getOptionalValue("mp.messaging.connector." + KafkaConnector.CONNECTOR_NAME + ".value.serializer",
                                String.class)
                        .isPresent();
            }
            return connectorHasValueSerializer;
        }

        if (key.startsWith("mp.messaging.incoming.") && key.endsWith(".key.deserializer")) {
            if (connectorHasKeyDeserializer == null) {
                connectorHasKeyDeserializer = getConfig()
                        .getOptionalValue("mp.messaging.connector." + KafkaConnector.CONNECTOR_NAME + ".key.deserializer",
                                String.class)
                        .isPresent();
            }
            return connectorHasKeyDeserializer;
        }
        if (key.startsWith("mp.messaging.incoming.") && key.endsWith(".value.deserializer")) {
            if (connectorHasValueDeserializer == null) {
                connectorHasValueDeserializer = getConfig()
                        .getOptionalValue("mp.messaging.connector." + KafkaConnector.CONNECTOR_NAME + ".value.deserializer",
                                String.class)
                        .isPresent();
            }
            return connectorHasValueDeserializer;
        }

        return false;
    }

    void ifNotYetConfigured(String key, Runnable runnable) {
        if (!alreadyConfigured.contains(key)) {
            alreadyConfigured.add(key);
            runnable.run();
        }
    }

    boolean isAvroGenerated(DotName className) {
        ClassInfo clazz = index.getClassByName(className);
        return clazz != null && clazz.declaredAnnotation(DotNames.AVRO_GENERATED) != null;
    }

    boolean hasConfluent() {
        if (hasConfluent == null) {
            try {
                Class.forName("io.confluent.kafka.serializers.KafkaAvroDeserializer", false,
                        Thread.currentThread().getContextClassLoader());
                hasConfluent = true;
            } catch (ClassNotFoundException e) {
                hasConfluent = false;
            }
        }

        return hasConfluent;
    }

    boolean hasApicurio1() {
        if (hasApicurio1 == null) {
            try {
                Class.forName("io.apicurio.registry.utils.serde.AvroKafkaDeserializer", false,
                        Thread.currentThread().getContextClassLoader());
                hasApicurio1 = true;
            } catch (ClassNotFoundException e) {
                hasApicurio1 = false;
            }
        }

        return hasApicurio1;
    }

    boolean hasApicurio2() {
        if (hasApicurio2 == null) {
            try {
                Class.forName("io.apicurio.registry.serde.avro.AvroKafkaDeserializer", false,
                        Thread.currentThread().getContextClassLoader());
                hasApicurio2 = true;
            } catch (ClassNotFoundException e) {
                hasApicurio2 = false;
            }
        }

        return hasApicurio2;
    }

    boolean hasJsonb() {
        if (hasJsonb == null) {
            try {
                Class.forName("javax.json.bind.Jsonb", false,
                        Thread.currentThread().getContextClassLoader());
                hasJsonb = true;
            } catch (ClassNotFoundException e) {
                hasJsonb = false;
            }
        }

        return hasJsonb;
    }

    ClassInfo getSubclassOfWithTypeArgument(DotName superclass, DotName expectedTypeArgument) {
        return index.getKnownDirectSubclasses(superclass)
                .stream()
                .filter(it -> it.superClassType().kind() == Type.Kind.PARAMETERIZED_TYPE
                        && it.superClassType().asParameterizedType().arguments().size() == 1
                        && it.superClassType().asParameterizedType().arguments().get(0).name().equals(expectedTypeArgument))
                .findAny()
                .orElse(null);
    }

    ClassInfo getImplementorOfWithTypeArgument(DotName implementedInterface, DotName expectedTypeArgument) {
        return index.getKnownDirectImplementors(implementedInterface)
                .stream()
                .filter(ci -> ci.interfaceTypes().stream()
                        .anyMatch(it -> it.name().equals(implementedInterface)
                                && it.kind() == Type.Kind.PARAMETERIZED_TYPE
                                && it.asParameterizedType().arguments().size() == 1
                                && it.asParameterizedType().arguments().get(0).name().equals(expectedTypeArgument)))
                .findAny()
                .orElse(null);
    }

    List<AnnotationInstance> findAnnotationsOnMethods(DotName annotation) {
        return index.getAnnotations(annotation)
                .stream()
                .filter(it -> it.target().kind() == AnnotationTarget.Kind.METHOD)
                .collect(Collectors.toList());
    }

    List<AnnotationInstance> findAnnotationsOnInjectionPoints(DotName annotation) {
        return index.getAnnotations(annotation)
                .stream()
                .filter(it -> it.target().kind() == AnnotationTarget.Kind.FIELD
                        || it.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER)
                .collect(Collectors.toList());
    }
}
