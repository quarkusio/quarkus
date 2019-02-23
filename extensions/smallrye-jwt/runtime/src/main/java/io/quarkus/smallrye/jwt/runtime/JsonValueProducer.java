package io.quarkus.smallrye.jwt.runtime;

import java.util.Optional;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.microprofile.jwt.Claim;
import org.jboss.logging.Logger;

/**
 * A producer for JsonValue injection types
 */
public class JsonValueProducer {
    private static Logger log = Logger.getLogger(JsonValueProducer.class);

    @Inject
    CommonJwtProducer util;

    @Produces
    @Claim("")
    public JsonString getJsonString(InjectionPoint ip) {
        return getValue(ip);
    }

    @Produces
    @Claim("")
    public Optional<JsonString> getOptionalJsonString(InjectionPoint ip) {
        return getOptionalValue(ip);
    }

    @Produces
    @Claim("")
    public JsonNumber getJsonNumber(InjectionPoint ip) {
        return getValue(ip);
    }

    @Produces
    @Claim("")
    public Optional<JsonNumber> getOptionalJsonNumber(InjectionPoint ip) {
        return getOptionalValue(ip);
    }

    @Produces
    @Claim("")
    public JsonArray getJsonArray(InjectionPoint ip) {
        return getValue(ip);
    }

    @Produces
    @Claim("")
    public Optional<JsonArray> getOptionalJsonArray(InjectionPoint ip) {
        return getOptionalValue(ip);
    }

    @Produces
    @Claim("")
    public JsonObject getJsonObject(InjectionPoint ip) {
        return getValue(ip);
    }

    @Produces
    @Claim("")
    public Optional<JsonObject> getOptionalJsonObject(InjectionPoint ip) {
        return getOptionalValue(ip);
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> T getValue(InjectionPoint ip) {
        log.debugf("JsonValueProducer(%s).produce", ip);
        T jsonValue = (T) util.generalJsonValueProducer(ip);
        return jsonValue;
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> Optional<T> getOptionalValue(InjectionPoint ip) {
        log.debugf("JsonValueProducer(%s).produce", ip);
        T jsonValue = (T) util.generalJsonValueProducer(ip);
        return Optional.ofNullable(jsonValue);
    }

}
