package io.quarkus.resteasy.reactive.links.deployment;

import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

/**
 * Shared helpers for deducing entity types and unwrapping async/reactive return types.
 */
final class RestLinksTypeUtil {

    private RestLinksTypeUtil() {
    }

    /**
     * If a method return type is parameterized and has a single argument (e.g. List<Foo>), then use that argument as the
     * entity type. Otherwise, use the return type itself.
     */
    static String deductEntityType(Type returnType) {
        if (returnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = returnType.asParameterizedType();
            if (parameterizedType.arguments().size() == 1) {
                return parameterizedType.arguments().get(0).name().toString();
            }
        }
        return returnType.name().toString();
    }

    /**
     * Unwrap common async/reactive wrappers to the underlying payload type.
     */
    static Type getNonAsyncReturnType(Type returnType) {
        switch (returnType.kind()) {
            case ARRAY:
            case CLASS:
            case PRIMITIVE:
            case VOID:
                return returnType;
            case PARAMETERIZED_TYPE:
                ParameterizedType parameterizedType = returnType.asParameterizedType();
                if (org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE
                        .equals(parameterizedType.name())
                        || org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETABLE_FUTURE
                                .equals(parameterizedType.name())
                        || org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI
                                .equals(parameterizedType.name())
                        || org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI
                                .equals(parameterizedType.name())
                        || org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_MULTI
                                .equals(parameterizedType.name())
                        || org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_RESPONSE
                                .equals(parameterizedType.name())) {
                    return parameterizedType.arguments().get(0);
                }
                return returnType;
            default:
        }
        return returnType;
    }
}
