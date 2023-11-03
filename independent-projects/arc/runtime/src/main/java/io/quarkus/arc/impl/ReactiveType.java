package io.quarkus.arc.impl;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * A method reactive returned type.
 */
public enum ReactiveType {

    /**
     * The method returns a non recognised reactive type
     */
    NON_REACTIVE(false, null),

    /**
     * The method returns a {@link Uni}
     */
    UNI(true, Uni.class),

    /**
     * The method returns a {@link Multi}
     */
    MULTI(true, Multi.class),

    /**
     * The method returns a {@link CompletionStage}
     */
    STAGE(true, CompletionStage.class);

    private final boolean reactive;
    private final Class<?> type;

    ReactiveType(boolean reactive, Class<?> type) {
        this.reactive = reactive;
        this.type = type;
    }

    boolean isReactive() {
        return reactive;
    }

    public static ReactiveType valueOf(Method method) {
        if (Void.class.equals(method.getReturnType())) {
            return NON_REACTIVE;
        }

        for (ReactiveType reactiveType : ReactiveType.values()) {
            if (Objects.nonNull(reactiveType.type)
                    && reactiveType.type.isAssignableFrom(method.getReturnType())) {
                return reactiveType;
            }
        }

        return NON_REACTIVE;
    }
}
