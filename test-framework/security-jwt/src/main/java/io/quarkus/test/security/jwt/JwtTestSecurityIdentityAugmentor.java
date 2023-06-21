package io.quarkus.test.security.jwt;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.TestSecurityIdentityAugmentor;

public class JwtTestSecurityIdentityAugmentor implements TestSecurityIdentityAugmentor {
    private static Converter<Long> longConverter = new LongConverter();
    private static Converter<Integer> intConverter = new IntegerConverter();
    private static Converter<Boolean> booleanConverter = new BooleanConverter();
    private static Map<String, Converter<?>> standardClaimConverteres = Map.of(
            Claims.exp.name(), longConverter,
            Claims.iat.name(), longConverter,
            Claims.nbf.name(), longConverter,
            Claims.auth_time.name(), longConverter,
            Claims.email_verified.name(), booleanConverter);

    private static Map<Claim.Type, Converter<?>> converters = Map.of(
            Claim.Type.STRING, new StringConverter(),
            Claim.Type.INTEGER, intConverter,
            Claim.Type.LONG, longConverter,
            Claim.Type.BOOLEAN, booleanConverter,
            Claim.Type.JSONARRAY, new JsonArrayConverter(),
            Claim.Type.JSONOBJECT, new JsonObjectConverter());

    @Override
    public SecurityIdentity augment(final SecurityIdentity identity, final Annotation[] annotations) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

        final JwtSecurity jwtSecurity = findJwtSecurity(annotations);
        builder.setPrincipal(new JsonWebToken() {

            @Override
            public String getName() {
                return identity.getPrincipal().getName();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getClaim(String claimName) {
                if (Claims.groups.name().equals(claimName)) {
                    return (T) identity.getRoles();
                }
                if (jwtSecurity != null && jwtSecurity.claims() != null) {
                    for (Claim claim : jwtSecurity.claims()) {
                        if (claim.key().equals(claimName)) {
                            return (T) wrapValue(claim, convertClaimValue(claim));
                        }
                    }
                }
                return null;
            }

            @Override
            public Set<String> getClaimNames() {
                if (jwtSecurity != null && jwtSecurity.claims() != null) {
                    return Arrays.stream(jwtSecurity.claims()).map(Claim::key).collect(Collectors.toSet());
                }
                return Collections.emptySet();
            }

        });

        return builder.build();
    }

    private static JwtSecurity findJwtSecurity(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            if (ann instanceof JwtSecurity) {
                return (JwtSecurity) ann;
            }
        }
        return null;
    }

    private Object wrapValue(Claim claim, Object convertedClaimValue) {
        Claims claimType = getClaimType(claim.key());
        if (Claims.UNKNOWN == claimType) {
            if (convertedClaimValue instanceof Long) {
                return Json.createValue((Long) convertedClaimValue);
            } else if (convertedClaimValue instanceof Integer) {
                return Json.createValue((Integer) convertedClaimValue);
            } else if (convertedClaimValue instanceof Boolean) {
                return (Boolean) convertedClaimValue ? JsonValue.TRUE : JsonValue.FALSE;
            }
        }
        return convertedClaimValue;
    }

    protected Claims getClaimType(String claimName) {
        Claims claimType;
        try {
            claimType = Claims.valueOf(claimName);
        } catch (IllegalArgumentException e) {
            claimType = Claims.UNKNOWN;
        }
        return claimType;
    }

    @SuppressWarnings("unchecked")
    private <T> T convertClaimValue(Claim claim) {
        if (claim.type() != Claim.Type.UNKNOWN) {
            Converter<?> converter = converters.get(claim.type());
            if (converter != null) {
                return (T) converter.convert(claim.value());
            } else {
                throw new RuntimeException("Unsupported claim type: " + claim.type().name());
            }
        } else if (standardClaimConverteres.containsKey(claim.key())) {
            Converter<?> converter = standardClaimConverteres.get(claim.key());
            return (T) converter.convert(claim.value());
        } else {
            return (T) claim.value();
        }
    }

    private static interface Converter<T> {
        T convert(String value);
    }

    private static class StringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    private static class IntegerConverter implements Converter<Integer> {
        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    private static class LongConverter implements Converter<Long> {
        @Override
        public Long convert(String value) {
            return Long.valueOf(value);
        }
    }

    private static class BooleanConverter implements Converter<Boolean> {
        @Override
        public Boolean convert(String value) {
            return Boolean.valueOf(value);
        }
    }

    private static class JsonObjectConverter implements Converter<JsonObject> {
        @Override
        public JsonObject convert(String value) {
            try (JsonReader jsonReader = Json.createReader(new StringReader(value))) {
                return jsonReader.readObject();
            }
        }
    }

    private static class JsonArrayConverter implements Converter<JsonArray> {
        @Override
        public JsonArray convert(String value) {
            try (JsonReader jsonReader = Json.createReader(new StringReader(value))) {
                return jsonReader.readArray();
            }
        }
    }
}
