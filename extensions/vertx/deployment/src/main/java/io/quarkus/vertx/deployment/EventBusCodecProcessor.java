package io.quarkus.vertx.deployment;

import static io.quarkus.vertx.deployment.VertxConstants.COMPLETION_STAGE;
import static io.quarkus.vertx.deployment.VertxConstants.CONSUME_EVENT;
import static io.quarkus.vertx.deployment.VertxConstants.LOCAL_EVENT_BUS_CODEC;
import static io.quarkus.vertx.deployment.VertxConstants.UNI;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class EventBusCodecProcessor {

    private static final Logger LOGGER = Logger.getLogger(EventBusCodecProcessor.class.getName());

    private static final DotName OBJECT = DotName.createSimple(Object.class);

    @BuildStep
    public void registerCodecs(
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<MessageCodecBuildItem> messageCodecs,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        final IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<AnnotationInstance> consumeEventAnnotationInstances = index.getAnnotations(CONSUME_EVENT);
        Map<DotName, DotName> codecByTypes = new HashMap<>();
        for (AnnotationInstance consumeEventAnnotationInstance : consumeEventAnnotationInstances) {
            AnnotationTarget typeTarget = consumeEventAnnotationInstance.target();
            if (typeTarget.kind() != AnnotationTarget.Kind.METHOD) {
                throw new UnsupportedOperationException("@ConsumeEvent annotation must target a method");
            }

            MethodInfo method = typeTarget.asMethod();
            Type codecTargetFromReturnType = extractPayloadTypeFromReturn(method);
            Type codecTargetFromParameter = extractPayloadTypeFromParameter(method);

            // If the @ConsumeEvent set the codec, use this codec. It applies to the parameter
            AnnotationValue codec = consumeEventAnnotationInstance.value("codec");
            if (codec != null && codec.asClass().kind() == Type.Kind.CLASS) {
                if (codecTargetFromParameter == null) {
                    throw new IllegalStateException("Invalid `codec` argument in @ConsumeEvent - no parameter");
                }
                codecByTypes.put(codecTargetFromParameter.name(), codec.asClass().asClassType().name());
            } else if (codecTargetFromParameter != null) {
                // Codec is not set, check if we have a built-in codec
                if (!hasBuiltInCodec(codecTargetFromParameter)) {
                    // Ensure local delivery.
                    AnnotationValue local = consumeEventAnnotationInstance.value("local");
                    if (local != null && !local.asBoolean()) {
                        throw new UnsupportedOperationException(
                                "The generic message codec can only be used for local delivery,"
                                        + ", implement your own event bus codec for " + codecTargetFromParameter.name()
                                                .toString());
                    } else if (!codecByTypes.containsKey(codecTargetFromParameter.name())) {
                        LOGGER.debugf("Local Message Codec registered for type %s",
                                codecTargetFromParameter);
                        codecByTypes.put(codecTargetFromParameter.name(), LOCAL_EVENT_BUS_CODEC);
                    }
                }
            }

            if (codecTargetFromReturnType != null && !hasBuiltInCodec(codecTargetFromReturnType)
                    && !codecByTypes.containsKey(codecTargetFromReturnType.name())) {

                LOGGER.debugf("Local Message Codec registered for type %s", codecTargetFromReturnType);
                codecByTypes.put(codecTargetFromReturnType.name(), LOCAL_EVENT_BUS_CODEC);
            }
        }

        // Produce the build items for registered types
        for (Map.Entry<DotName, DotName> entry : codecByTypes.entrySet()) {
            messageCodecs.produce(new MessageCodecBuildItem(entry.getKey().toString(), entry.getValue().toString()));
        }

        // Produce the build items for subclasses of registered types
        // But do not override the existing ones
        for (Map.Entry<DotName, DotName> entry : codecByTypes.entrySet()) {
            // we do not consider Object as it would be a mess
            if (OBJECT.equals(entry.getKey())) {
                continue;
            }

            Set<DotName> subclasses = combinedIndex.getIndex().getAllKnownSubclasses(entry.getKey()).stream()
                    .map(ci -> ci.name())
                    .filter(d -> !codecByTypes.containsKey(d))
                    .collect(Collectors.toSet());

            for (DotName subclass : subclasses) {
                messageCodecs.produce(new MessageCodecBuildItem(subclass.toString(), entry.getValue().toString()));
            }
        }

        // Register codec classes for reflection.
        codecByTypes.values().stream().map(DotName::toString).distinct()
                .forEach(new Consumer<String>() {
                    @Override
                    public void accept(String name) {
                        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, name));
                    }
                });
    }

    private static final List<String> BUILT_IN_CODECS = Arrays.asList(
            // Primitive wrapper classes
            Boolean.class.getName(),
            Byte.class.getName(),
            Character.class.getName(),
            Double.class.getName(),
            Integer.class.getName(),
            Float.class.getName(),
            Long.class.getName(),
            Short.class.getName(),

            String.class.getName(),

            Void.class.getName(),

            JsonObject.class.getName(),
            JsonArray.class.getName(),

            // Buffers classes
            Buffer.class.getName(),
            io.vertx.mutiny.core.buffer.Buffer.class.getName());

    private static Type extractPayloadTypeFromReturn(MethodInfo method) {
        Type returnType = method.returnType();
        if (returnType.kind() == Type.Kind.CLASS) {
            return returnType;
        } else if (returnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType returnedParamType = returnType.asParameterizedType();
            if (!returnedParamType.arguments().isEmpty()
                    && (returnedParamType.name().equals(COMPLETION_STAGE) || returnedParamType.name().equals(UNI))) {
                return returnedParamType.arguments().get(0);
            } else {
                return returnedParamType;
            }
        }
        return null;
    }

    private static Type extractPayloadTypeFromParameter(MethodInfo method) {
        List<Type> parameters = method.parameterTypes();
        if (parameters.isEmpty()) {
            return null;
        }
        Type param = method.parameterType(0);
        if (param.kind() == Type.Kind.CLASS) {
            return param;
        } else if (param.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterType = param.asParameterizedType();
            if (isMessageClass(parameterType) && !parameterType.arguments().isEmpty()) {
                return parameterType.arguments().get(0);
            } else {
                return parameterType;
            }
        }
        return null;
    }

    /**
     * Checks whether the given type has a built-in codec.
     *
     * @param type the type, must not be {@code null}
     * @return {@code true} is the type has a built-in codec, {@code false} otherwise.
     */
    private static boolean hasBuiltInCodec(Type type) {
        if (type.kind() == Type.Kind.PRIMITIVE) {
            return true;
        }
        String typeAsString = type.name().toString();
        return BUILT_IN_CODECS.contains(typeAsString);
    }

    /**
     * Checks whether the given type matches one of the event bus {@code Message} classes.
     *
     * @param type the type, must not be {@code null}
     * @return {@code true} if it matches, {@code false} otherwise.
     */
    private static boolean isMessageClass(ParameterizedType type) {
        return VertxConstants.isMessage(type.name());
    }
}
