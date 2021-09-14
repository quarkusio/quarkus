package io.quarkus.qute;

import io.quarkus.qute.TemplateNode.Origin;

/**
 * This component can be used to map a result of an evaluated value expression to a string value.
 * <p>
 * The first result mapper that applies to the result object is used. The mapper with higher priority wins.
 * 
 * @see Engine#getResultMappers()
 * @see Engine#mapResult(Object, Expression)
 */
@FunctionalInterface
public interface ResultMapper extends WithPriority {

    /**
     * 
     * @param origin
     * @param result
     * @return {@code true} if this mapper applies to the given result
     */
    default boolean appliesTo(Origin origin, Object result) {
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
