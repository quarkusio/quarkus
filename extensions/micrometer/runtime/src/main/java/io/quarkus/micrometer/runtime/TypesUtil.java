package io.quarkus.micrometer.runtime;

import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Uni;

final class TypesUtil {

    private TypesUtil() {
    }

    static boolean isUni(Class<?> clazz) {
        return Uni.class.isAssignableFrom(clazz);
    }

    static boolean isCompletionStage(Class<?> clazz) {
        return CompletionStage.class.isAssignableFrom(clazz);
    }
}
