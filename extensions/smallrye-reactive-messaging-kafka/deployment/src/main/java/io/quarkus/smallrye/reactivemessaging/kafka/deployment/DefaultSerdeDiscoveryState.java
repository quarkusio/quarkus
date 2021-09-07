package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.smallrye.reactive.messaging.kafka.KafkaConnector;

class DefaultSerdeDiscoveryState {
    private final IndexView index;

    private final Map<String, Boolean> isKafkaConnector = new HashMap<>();
    private final Set<String> alreadyConfigured = new HashSet<>();

    private Boolean hasConfluent;
    private Boolean hasApicurio1;
    private Boolean hasApicurio2;
    private Boolean hasJsonb;
    final boolean onlyKafka;

    DefaultSerdeDiscoveryState(IndexView index, boolean onlyKafka) {
        this.index = index;
        this.onlyKafka = onlyKafka;
    }

    boolean isKafkaConnector(boolean incoming, String channelName) {
        String channelType = incoming ? "incoming" : "outgoing";
        return isKafkaConnector.computeIfAbsent(channelType + "|" + channelName, ignored -> {
            String connectorKey = "mp.messaging." + channelType + "." + channelName + ".connector";
            String connector = ConfigProvider.getConfig()
                    .getOptionalValue(connectorKey, String.class)
                    .orElse(onlyKafka ? KafkaConnector.CONNECTOR_NAME : "ignored");
            return KafkaConnector.CONNECTOR_NAME.equals(connector);
        });
    }

    void ifNotYetConfigured(String key, Runnable runnable) {
        if (!alreadyConfigured.contains(key)) {
            alreadyConfigured.add(key);
            runnable.run();
        }
    }

    boolean isAvroGenerated(DotName className) {
        ClassInfo clazz = index.getClassByName(className);
        return clazz != null && clazz.classAnnotation(DotNames.AVRO_GENERATED) != null;
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
