package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

public interface ExchangeAttributeSerializable {

    public Map<String, Optional<String>> serialize(RoutingContext exchange);
}
