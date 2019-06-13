package io.quarkus.builder;

/**
 */
public enum ProduceFlag {
    /**
     * Only produce this item weakly: if only weak items produced by a build step are consumed, the step will not be included.
     */
    WEAK,
}
