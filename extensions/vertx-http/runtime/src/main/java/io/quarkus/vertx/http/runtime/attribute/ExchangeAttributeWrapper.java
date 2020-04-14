package io.quarkus.vertx.http.runtime.attribute;

/**
 * Interface that can be used to wrap an exchange attribute.
 *
 */
public interface ExchangeAttributeWrapper {

    ExchangeAttribute wrap(ExchangeAttribute attribute);

}
