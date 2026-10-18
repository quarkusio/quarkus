package io.quarkus.vertx.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;

/**
 * Identifies a route method parameter that should be injected with a value returned from:
 * <ul>
 * <li>{@link RequestBody#buffer()} for type {@link Buffer}</li>
 * <li>{@link RequestBody#asString()} for type {@link String}</li>
 * <li>{@link RequestBody#asJsonObject()} for type {@link JsonObject}</li>
 * <li>{@link RequestBody#asJsonArray()} for type {@link JsonArray}</li>
 * <li>{{@link JsonObject#mapTo(Class)} for any other type</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Body {

}
