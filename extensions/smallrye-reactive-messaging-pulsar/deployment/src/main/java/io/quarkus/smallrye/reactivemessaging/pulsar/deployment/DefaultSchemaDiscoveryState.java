package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import static io.quarkus.smallrye.reactivemessaging.deployment.SmallRyeReactiveMessagingProcessor.getChannelPropertyKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import io.smallrye.reactive.messaging.pulsar.PulsarConnector;

class DefaultSchemaDiscoveryState {
    private final IndexView index;

    private final Map<String, Boolean> isPulsarConnector = new HashMap<>();
    private final Set<String> alreadyConfigured = new HashSet<>();

    private Boolean connectorHasSchema;

    DefaultSchemaDiscoveryState(IndexView index) {
        this.index = index;
    }

    Config getConfig() {
        return ConfigProvider.getConfig();
    }

    boolean isPulsarConnector(List<ConnectorManagedChannelBuildItem> channelsManagedByConnectors, boolean incoming,
            String channelName) {
        // First look in the channelsManagedByConnectors list
        Optional<ConnectorManagedChannelBuildItem> match = channelsManagedByConnectors.stream()
                .filter(cn -> cn.getDirection() == (incoming ? ChannelDirection.INCOMING : ChannelDirection.OUTGOING)
                        && cn.getName().equalsIgnoreCase(channelName))
                .findFirst();
        if (match.isPresent()) {
            return true;
        }

        String channelType = incoming ? "incoming" : "outgoing";
        return isPulsarConnector.computeIfAbsent(channelType + "|" + channelName, ignored -> {
            String connectorKey = getChannelPropertyKey(channelName, "connector", incoming);
            String connector = getConfig().getOptionalValue(connectorKey, String.class).orElse("ignored");
            return PulsarConnector.CONNECTOR_NAME.equals(connector);
        });
    }

    boolean shouldNotConfigure(String key) {
        // if we know at build time that schema is configured on the connector,
        // we should NOT emit default configuration for schema on the channel
        // (in other words, only a user can explicitly override a connector configuration)
        //
        // more config properties could possibly be handled in the same way, but these should suffice for now

        if (key.startsWith("mp.messaging.outgoing.") && key.endsWith(".schema")) {
            if (connectorHasSchema == null) {
                connectorHasSchema = getConfig()
                        .getOptionalValue("mp.messaging.connector." + PulsarConnector.CONNECTOR_NAME + ".schema",
                                String.class)
                        .isPresent();
            }
            return connectorHasSchema;
        }

        if (key.startsWith("mp.messaging.incoming.") && key.endsWith(".schema")) {
            if (connectorHasSchema == null) {
                connectorHasSchema = getConfig()
                        .getOptionalValue("mp.messaging.connector." + PulsarConnector.CONNECTOR_NAME + ".schema",
                                String.class)
                        .isPresent();
            }
            return connectorHasSchema;
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

    boolean isProtobufGenerated(DotName className) {
        ClassInfo clazz = index.getClassByName(className);
        return clazz != null && Objects.equals(clazz.superName(), DotNames.PROTOBUF_GENERATED);
    }

    boolean hasObjectMapperConfigSchema(Type type, String channelName, boolean incoming) {
        String key = getChannelPropertyKey(channelName, "schema", incoming);
        Optional<String> schema = getConfig().getOptionalValue(key, String.class);
        return schema.isPresent() && schema.get().equals(SyntheticBeanBuilder.objectMapperSchemaId(type));
    }

    List<AnnotationInstance> findAnnotationsOnMethods(DotName annotation) {
        return index.getAnnotations(annotation).stream()
                .filter(it -> it.target().kind() == AnnotationTarget.Kind.METHOD).collect(Collectors.toList());
    }

    List<AnnotationInstance> findRepeatableAnnotationsOnMethods(DotName annotation) {
        return index.getAnnotationsWithRepeatable(annotation, index).stream()
                .filter(it -> it.target().kind() == AnnotationTarget.Kind.METHOD).collect(Collectors.toList());
    }

    List<AnnotationInstance> findAnnotationsOnInjectionPoints(DotName annotation) {
        return index.getAnnotations(annotation).stream().filter(it -> it.target().kind() == AnnotationTarget.Kind.FIELD
                || it.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER).collect(Collectors.toList());
    }

    List<AnnotationInstance> findProvidedSchemaWithIdentifier(String identifier) {
        return index.getAnnotations(DotNames.IDENTIFIER).stream()
                .filter(it -> it.target().kind() == AnnotationTarget.Kind.FIELD
                        || it.target().kind() == AnnotationTarget.Kind.METHOD)
                .filter(a -> a.target().hasAnnotation(DotNames.PRODUCES)).filter(a -> {
                    AnnotationTarget target = a.target();
                    if (target.kind() == AnnotationTarget.Kind.FIELD) {
                        return target.asField().type().name().equals(DotNames.PULSAR_SCHEMA);
                    }
                    if (target.kind() == AnnotationTarget.Kind.METHOD) {
                        return target.asMethod().returnType().name().equals(DotNames.PULSAR_SCHEMA);
                    }
                    return false;
                }).filter(a -> Objects.equals(identifier, a.value().asString())).collect(Collectors.toList());
    }

    List<ClassInfo> findImplementedSchemaWithIdentifier(String identifier) {
        return index.getAllKnownImplementors(DotNames.PULSAR_SCHEMA).stream()
                .filter(t -> t.hasAnnotation(DotNames.IDENTIFIER)
                        && Objects.equals(t.annotation(DotNames.IDENTIFIER).value().asString(), identifier))
                .collect(Collectors.toList());
    }
}
