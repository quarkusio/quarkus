package io.quarkus.smallrye.jwt.runtime;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@RequestScoped
public class CommonJwtProducer {
    private static Logger log = Logger.getLogger(CommonJwtProducer.class);
    private static final String TMP = "tmp";

    @Inject
    JsonWebToken currentToken;

    /**
     * A utility method for accessing a claim from the current JsonWebToken as a ClaimValue<Optional<T>> object.
     *
     * @param ip - injection point of the claim
     * @param <T> expected actual type of the claim
     * @return the claim value wrapper object
     */
    public <T> ClaimValue<Optional<T>> generalClaimValueProducer(InjectionPoint ip) {
        String name = getName(ip);
        ClaimValueWrapper<Optional<T>> wrapper = new ClaimValueWrapper<>(name);
        T value = getValue(name, false);
        Optional<T> optValue = Optional.ofNullable(value);
        wrapper.setValue(optValue);
        return wrapper;
    }

    /**
     * Return the indicated claim value as a JsonValue
     *
     * @param ip - injection point of the claim
     * @return a JsonValue wrapper
     */
    public JsonValue generalJsonValueProducer(InjectionPoint ip) {
        String name = getName(ip);
        Object value = getValue(name, false);
        JsonValue jsonValue = wrapValue(value);
        return jsonValue;
    }

    public <T> T getValue(String name, boolean isOptional) {
        if (currentToken == null) {
            log.debugf("getValue(%s), null JsonWebToken", name);
            return null;
        }

        Optional<T> claimValue = currentToken.claim(name);
        if (!isOptional && !claimValue.isPresent()) {
            log.debugf("Failed to find Claim for: %s", name);
        }
        log.debugf("getValue(%s), isOptional=%s, claimValue=%s", name, isOptional, claimValue);
        return claimValue.orElse(null);
    }

    public String getName(InjectionPoint ip) {
        String name = null;
        for (Annotation ann : ip.getQualifiers()) {
            if (ann instanceof Claim) {
                Claim claim = (Claim) ann;
                name = claim.standard() == Claims.UNKNOWN ? claim.value() : claim.standard().name();
            }
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    private static JsonObject replaceMap(Map<String, Object> map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object entryValue = entry.getValue();
            if (entryValue instanceof Map) {
                JsonObject entryJsonObject = replaceMap((Map<String, Object>) entryValue);
                builder.add(entry.getKey(), entryJsonObject);
            } else if (entryValue instanceof List) {
                JsonArray array = (JsonArray) wrapValue(entryValue);
                builder.add(entry.getKey(), array);
            } else if (entryValue instanceof Long || entryValue instanceof Integer) {
                long lvalue = ((Number) entryValue).longValue();
                builder.add(entry.getKey(), lvalue);
            } else if (entryValue instanceof Double || entryValue instanceof Float) {
                double dvalue = ((Number) entryValue).doubleValue();
                builder.add(entry.getKey(), dvalue);
            } else if (entryValue instanceof Boolean) {
                boolean flag = ((Boolean) entryValue).booleanValue();
                builder.add(entry.getKey(), flag);
            } else if (entryValue instanceof String) {
                builder.add(entry.getKey(), entryValue.toString());
            }
        }
        return builder.build();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static JsonValue wrapValue(Object value) {
        JsonValue jsonValue = null;
        if (value instanceof JsonValue) {
            // This may already be a JsonValue
            jsonValue = (JsonValue) value;
        } else if (value instanceof String) {
            jsonValue = Json.createObjectBuilder()
                    .add(TMP, value.toString())
                    .build()
                    .getJsonString(TMP);
        } else if (value instanceof Number) {
            Number number = (Number) value;
            if ((number instanceof Long) || (number instanceof Integer)) {
                jsonValue = Json.createObjectBuilder()
                        .add(TMP, number.longValue())
                        .build()
                        .getJsonNumber(TMP);
            } else {
                jsonValue = Json.createObjectBuilder()
                        .add(TMP, number.doubleValue())
                        .build()
                        .getJsonNumber(TMP);
            }
        } else if (value instanceof Boolean) {
            Boolean flag = (Boolean) value;
            jsonValue = flag ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (value instanceof Collection) {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            Collection list = (Collection) value;
            for (Object element : list) {
                if (element instanceof String) {
                    arrayBuilder.add(element.toString());
                } else {
                    JsonValue jvalue = wrapValue(element);
                    arrayBuilder.add(jvalue);
                }
            }
            jsonValue = arrayBuilder.build();
        } else if (value instanceof Map) {
            jsonValue = replaceMap((Map) value);
        }
        return jsonValue;
    }
}
