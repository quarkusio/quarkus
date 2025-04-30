package io.quarkus.qute;

@FunctionalInterface
interface AccessorCandidate {

    /**
     *
     * @param context
     * @return an accessor, is never null
     */
    ValueAccessor getAccessor(EvalContext context);

    /**
     * If the accessor can be shared and does not need to access the {@link EvalContext}
     * <p>
     * For example, getters and field accessors are stateless.
     *
     * @return {@code true} if the accessor can be shared, {@code false} otherwise
     */
    default boolean isShared(EvalContext context) {
        return true;
    }

}
