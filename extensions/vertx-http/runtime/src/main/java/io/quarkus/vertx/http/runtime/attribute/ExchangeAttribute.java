package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * Representation of a string attribute from a HTTP server exchange.
 *
 */
public interface ExchangeAttribute {

    /**
     * Resolve the attribute from the HTTP server exchange. This may return null if the attribute is not present.
     * 
     * @param exchange The exchange
     * @return The attribute
     */
    String readAttribute(final RoutingContext exchange);

    /**
     * Sets a new value for the attribute. Not all attributes are writable.
     * 
     * @param exchange The exchange
     * @param newValue The new value for the attribute
     * @throws ReadOnlyAttributeException when attribute cannot be written
     */
    void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException;
}
