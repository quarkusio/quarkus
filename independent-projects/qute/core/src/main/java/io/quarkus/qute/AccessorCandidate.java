package io.quarkus.qute;

@FunctionalInterface
interface AccessorCandidate {

    /**
     * 
     * @param context
     * @return an accessor, is never null
     */
    ValueAccessor getAccessor(EvalContext context);

}
