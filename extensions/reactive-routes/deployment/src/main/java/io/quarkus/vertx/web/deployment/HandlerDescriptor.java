package io.quarkus.vertx.web.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.hibernate.validator.spi.BeanValidationAnnotationsBuildItem;

/**
 * Describe a request handler.
 */
class HandlerDescriptor {

    private final MethodInfo method;
    private final BeanValidationAnnotationsBuildItem validationAnnotations;
    private final boolean failureHandler;
    private final Type payloadType;
    private final String[] contentTypes;

    HandlerDescriptor(MethodInfo method, BeanValidationAnnotationsBuildItem bvAnnotations, boolean failureHandler,
            String[] producedTypes) {
        this.method = method;
        this.validationAnnotations = bvAnnotations;
        this.failureHandler = failureHandler;
        Type returnType = method.returnType();
        if (returnType.kind() == Kind.VOID) {
            payloadType = null;
        } else {
            if (returnType.name().equals(DotNames.UNI) || returnType.name().equals(DotNames.MULTI)
                    || returnType.name().equals(DotNames.COMPLETION_STAGE)) {
                payloadType = returnType.asParameterizedType().arguments().get(0);
            } else {
                payloadType = returnType;
            }
        }
        this.contentTypes = producedTypes;
    }

    Type getReturnType() {
        return method.returnType();
    }

    boolean isReturningUni() {
        return method.returnType().name().equals(DotNames.UNI);
    }

    boolean isReturningMulti() {
        return method.returnType().name().equals(DotNames.MULTI);
    }

    boolean isReturningCompletionStage() {
        return method.returnType().name().equals(DotNames.COMPLETION_STAGE);
    }

    public String getFirstContentType() {
        if (contentTypes == null || contentTypes.length == 0) {
            return null;
        }
        return contentTypes[0];
    }

    /**
     * @return {@code true} if the method is annotated with a constraint or {@code @Valid} or any parameter has such kind of
     *         annotation.
     */
    boolean requireValidation() {
        if (validationAnnotations == null) {
            return false;
        }
        for (AnnotationInstance annotation : method.annotations()) {
            if (validationAnnotations.getAllAnnotations().contains(annotation.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if the method is annotated with {@code @Valid}.
     */
    boolean isProducedResponseValidated() {
        if (validationAnnotations == null) {
            return false;
        }
        for (AnnotationInstance annotation : method.annotations()) {
            if (validationAnnotations.getValidAnnotation().equals(annotation.name())) {
                return true;
            }
        }
        return false;
    }

    Type getPayloadType() {
        return payloadType;
    }

    boolean isPayloadString() {
        Type type = getPayloadType();
        if (type == null) {
            return false;
        }
        return type.name().equals(io.quarkus.arc.processor.DotNames.STRING);
    }

    boolean isPayloadTypeBuffer() {
        Type type = getPayloadType();
        if (type == null) {
            return false;
        }
        return type.name().equals(DotNames.BUFFER);
    }

    boolean isPayloadMutinyBuffer() {
        Type type = getPayloadType();
        if (type == null) {
            return false;
        }
        return type.name().equals(DotNames.MUTINY_BUFFER);
    }

    boolean isFailureHandler() {
        return failureHandler;
    }

}
