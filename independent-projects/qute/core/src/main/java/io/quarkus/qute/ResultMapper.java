package io.quarkus.qute;

/**
 * The first result mapper that applies to the result object is used to map the result to the string value. The mapper with
 * higher priority wins.
 */
@FunctionalInterface
public interface ResultMapper extends WithPriority {

    /**
     * 
     * @param result
     * @return {@code true} if this mapper applies to the given result
     */
    default boolean appliesTo(Object result) {
        return true;
    }

    /**
     * 
     * @param result
     * @param expression The original expression
     * @return the string value
     */
    String map(Object result, Expression expression);

}
