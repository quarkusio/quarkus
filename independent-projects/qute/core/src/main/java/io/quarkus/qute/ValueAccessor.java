package io.quarkus.qute;

import java.util.concurrent.CompletionStage;

/**
 *
 * @see ReflectionValueResolver
 */
@FunctionalInterface
interface ValueAccessor {

    /**
     *
     * @param instance
     * @return the value
     */
    CompletionStage<Object> getValue(Object instance);

}
