package io.quarkus.vertx.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Identifies a route method parameter that should be injected with a value returned from:
 * <ul>
 * <li>{@link RoutingContext#getBody()} for type {@link Buffer}</li>
 * <li>{@link RoutingContext#getBodyAsString()} for type {@link String}</li>
 * <li>{@link RoutingContext#getBodyAsJson()} for type {@link JsonObject}</li>
 * <li>{@link RoutingContext#getBodyAsJsonArray()} for type {@link JsonArray}</li>
 * <li>{@link RoutingContext#getBodyAsJson()} and {@link JsonObject#mapTo(Class)} for any other type</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Body {

}
