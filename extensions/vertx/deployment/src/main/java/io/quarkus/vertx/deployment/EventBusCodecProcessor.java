package io.quarkus.vertx.deployment;

import static io.quarkus.vertx.deployment.VertxConstants.AXLE_MESSAGE;
import static io.quarkus.vertx.deployment.VertxConstants.COMPLETION_STAGE;
import static io.quarkus.vertx.deployment.VertxConstants.CONSUME_EVENT;
import static io.quarkus.vertx.deployment.VertxConstants.LOCAL_EVENT_BUS_CODEC;
import static io.quarkus.vertx.deployment.VertxConstants.MESSAGE;
import static io.quarkus.vertx.deployment.VertxConstants.RX_MESSAGE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class EventBusCodecProcessor {

    private static final Logger LOGGER = Logger.getLogger(EventBusCodecProcessor.class.getName());

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    public void registerCodecs(
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<MessageCodecBuildItem> messageCodecs) {

        final IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<AnnotationInstance> consumeEventAnnotationInstances = index.getAnnotations(CONSUME_EVENT);
        Map<Type, DotName> codecByTypes = new HashMap<>();
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
                codecByTypes.put(codecTargetFromParameter, codec.asClass().asClassType().name());
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
                    } else if (!codecByTypes.containsKey(codecTargetFromParameter)) {
                        LOGGER.infof("Local Message Codec registered for type %s",
                                codecTargetFromParameter.toString());
                        codecByTypes.put(codecTargetFromParameter, LOCAL_EVENT_BUS_CODEC);
                    }
                }
            }

            if (codecTargetFromReturnType != null && !hasBuiltInCodec(codecTargetFromReturnType)
                    && !codecByTypes.containsKey(codecTargetFromReturnType)) {

                LOGGER.infof("Local Message Codec registered for type %s", codecTargetFromReturnType.toString());
                codecByTypes.put(codecTargetFromReturnType, LOCAL_EVENT_BUS_CODEC);
            }
        }

        // Produce the build items
        for (Map.Entry<Type, DotName> entry : codecByTypes.entrySet()) {
            messageCodecs.produce(new MessageCodecBuildItem(entry.getKey().toString(), entry.getValue().toString()));
        }

        // Register codec classes for reflection.
        codecByTypes.values().stream().map(DotName::toString).distinct()
                .forEach(name -> reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, name)));
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
            io.vertx.axle.core.buffer.Buffer.class.getName(),
            io.vertx.reactivex.core.buffer.Buffer.class.getName());

    private static Type extractPayloadTypeFromReturn(MethodInfo method) {
        Type returnType = method.returnType();
        if (returnType.kind() == Type.Kind.CLASS) {
            return returnType;
        } else if (returnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType returnedParamType = returnType.asParameterizedType();
            if (!returnedParamType.arguments().isEmpty() && (returnedParamType.name().equals(COMPLETION_STAGE))) {
                return returnedParamType.arguments().get(0);
            } else {
                return returnedParamType;
            }
        }
        return null;
    }

    private static Type extractPayloadTypeFromParameter(MethodInfo method) {
        List<Type> parameters = method.parameters();
        if (parameters.isEmpty()) {
            return null;
        }
        Type param = method.parameters().get(0);
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
        return type.name().equals(MESSAGE)
                || type.name().equals(RX_MESSAGE)
                || type.name().equals(AXLE_MESSAGE);
    }
}
