package io.quarkus.arc;

/**
 * This interface is used by synthetic interceptor to initialize an interceptor instance.
 */
public interface InterceptorCreator {

    /**
     *
     * @param context
     * @return the intercept function
     */
    InterceptFunction create(SyntheticCreationalContext<Object> context);

    @FunctionalInterface
    public interface InterceptFunction {

        /**
         * The returned value is ignored by the container when the method is invoked to interpose on a lifecycle event.
         *
         * @param invocationContext
         * @return the return value
         * @throws Exception
         */
        Object intercept(ArcInvocationContext invocationContext) throws Exception;

    }

}
