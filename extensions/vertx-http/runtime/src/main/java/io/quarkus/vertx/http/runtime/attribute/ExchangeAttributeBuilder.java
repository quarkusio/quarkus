package io.quarkus.vertx.http.runtime.attribute;

/**
 * An interface that knows how to build an exchange attribute from a textual representation.
 * <p>
 * This makes it easy to configure attributes based on a string representation
 *
 */
public interface ExchangeAttributeBuilder {

    /**
     * The string representation of the attribute name. This is used solely for debugging / informational purposes
     *
     * @return The attribute name
     */
    String name();

    /**
     * Build the attribute from a text based representation. If the attribute does not understand this representation then
     * it will just return null.
     *
     * @param token The string token
     * @return The exchange attribute, or null
     */
    ExchangeAttribute build(final String token);

    /**
     * The priority of the builder. Builders will be tried in priority builder. Built in builders use the priority range 0-100,
     *
     * @return The priority
     */
    int priority();

}
