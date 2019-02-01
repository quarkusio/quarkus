package org.jboss.shamrock.jwt.runtime;


import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

/**
 * A producer for JsonValue injection types
 */
public class JsonValueProducer {
    private static Logger log = Logger.getLogger(JsonValueProducer.class);

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

    public <T extends JsonValue> T getValue(InjectionPoint ip) {
        log.debugf("JsonValueProducer(%s).produce", ip);
        String name = getName(ip);
        T jsonValue = (T) MPJWTProducer.generalJsonValueProducer(name);
        return jsonValue;
    }
    public <T extends JsonValue> Optional<T> getOptionalValue(InjectionPoint ip) {
        log.debugf("JsonValueProducer(%s).produce", ip);
        String name = getName(ip);
        T jsonValue = (T) MPJWTProducer.generalJsonValueProducer(name);
        return Optional.ofNullable(jsonValue);
    }

    String getName(InjectionPoint ip) {
        String name = null;
        for (Annotation ann : ip.getQualifiers()) {
            if (ann instanceof Claim) {
                Claim claim = (Claim) ann;
                name = claim.standard() == Claims.UNKNOWN ? claim.value() : claim.standard().name();
            }
        }
        return name;
    }
}
