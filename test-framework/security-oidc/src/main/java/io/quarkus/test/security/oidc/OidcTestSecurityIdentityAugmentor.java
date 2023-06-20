package io.quarkus.test.security.oidc;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.TestSecurityIdentityAugmentor;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;
import io.vertx.core.json.JsonObject;

public class OidcTestSecurityIdentityAugmentor implements TestSecurityIdentityAugmentor {

    private static Converter<Long> longConverter = new LongConverter();
    private static Converter<Integer> intConverter = new IntegerConverter();
    private static Converter<Boolean> booleanConverter = new BooleanConverter();
    private static Map<String, Converter<?>> standardClaimConverteres = Map.of(
            Claims.exp.name(), longConverter,
            Claims.iat.name(), longConverter,
            Claims.nbf.name(), longConverter,
            Claims.auth_time.name(), longConverter,
            Claims.email_verified.name(), booleanConverter);

    private static Map<Class<?>, Converter<?>> converters = Map.of(
            String.class, new StringConverter(),
            Integer.class, intConverter,
            int.class, intConverter,
            Long.class, longConverter,
            long.class, longConverter,
            Boolean.class, booleanConverter,
            boolean.class, booleanConverter,
            JsonArray.class, new JsonArrayConverter(),
            jakarta.json.JsonObject.class, new JsonObjectConverter());

    private Optional<String> issuer;
    private PrivateKey privateKey;

    public OidcTestSecurityIdentityAugmentor(Optional<String> issuer) {
        this.issuer = issuer;
        try {
            privateKey = KeyUtils.generateKeyPair(2048).getPrivate();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public SecurityIdentity augment(final SecurityIdentity identity, final Annotation[] annotations) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

        final OidcSecurity oidcSecurity = findOidcSecurity(annotations);

        final boolean introspectionRequired = oidcSecurity != null && oidcSecurity.introspectionRequired();

        if (!introspectionRequired) {
            // JsonWebToken
            JsonObjectBuilder claims = Json.createObjectBuilder();
            claims.add(Claims.preferred_username.name(), identity.getPrincipal().getName());
            claims.add(Claims.groups.name(),
                    Json.createArrayBuilder(identity.getRoles().stream().collect(Collectors.toList())).build());
            if (oidcSecurity != null && oidcSecurity.claims() != null) {
                for (Claim claim : oidcSecurity.claims()) {
                    Object claimValue = convertClaimValue(claim);
                    if (claimValue instanceof String) {
                        claims.add(claim.key(), (String) claimValue);
                    } else if (claimValue instanceof Long) {
                        claims.add(claim.key(), (Long) claimValue);
                    } else if (claimValue instanceof Integer) {
                        claims.add(claim.key(), (Integer) claimValue);
                    } else if (claimValue instanceof Boolean) {
                        claims.add(claim.key(), (Boolean) claimValue);
                    } else if (claimValue instanceof JsonArray) {
                        claims.add(claim.key(), (JsonArray) claimValue);
                    } else if (claimValue instanceof jakarta.json.JsonObject) {
                        claims.add(claim.key(), (jakarta.json.JsonObject) claimValue);
                    }
                }
            }
            jakarta.json.JsonObject claimsJson = claims.build();
            String jwt = generateToken(claimsJson);
            IdTokenCredential idToken = new IdTokenCredential(jwt);
            AccessTokenCredential accessToken = new AccessTokenCredential(jwt);

            try {
                JsonWebToken principal = new OidcJwtCallerPrincipal(JwtClaims.parse(claimsJson.toString()), idToken);
                builder.setPrincipal(principal);
            } catch (Exception ex) {
                throw new RuntimeException();
            }
            builder.addCredential(idToken);
            builder.addCredential(accessToken);
        } else {
            JsonObjectBuilder introspectionBuilder = Json.createObjectBuilder();
            introspectionBuilder.add(OidcConstants.INTROSPECTION_TOKEN_ACTIVE, true);
            introspectionBuilder.add(OidcConstants.INTROSPECTION_TOKEN_USERNAME, identity.getPrincipal().getName());
            introspectionBuilder.add(OidcConstants.TOKEN_SCOPE,
                    identity.getRoles().stream().collect(Collectors.joining(" ")));

            if (oidcSecurity != null && oidcSecurity.introspection() != null) {
                for (TokenIntrospection introspection : oidcSecurity.introspection()) {
                    introspectionBuilder.add(introspection.key(), introspection.value());
                }
            }

            builder.addAttribute(OidcUtils.INTROSPECTION_ATTRIBUTE,
                    new io.quarkus.oidc.TokenIntrospection(introspectionBuilder.build()));
            builder.addCredential(new AccessTokenCredential(UUID.randomUUID().toString(), null));
        }

        // UserInfo
        if (oidcSecurity != null && oidcSecurity.userinfo() != null) {
            JsonObjectBuilder userInfoBuilder = Json.createObjectBuilder();
            for (UserInfo userinfo : oidcSecurity.userinfo()) {
                userInfoBuilder.add(userinfo.key(), userinfo.value());
            }
            builder.addAttribute(OidcUtils.USER_INFO_ATTRIBUTE, new io.quarkus.oidc.UserInfo(userInfoBuilder.build()));
        }

        // OidcConfigurationMetadata
        JsonObject configMetadataBuilder = new JsonObject();
        if (issuer.isPresent()) {
            configMetadataBuilder.put("issuer", issuer.get());
        }
        if (oidcSecurity != null && oidcSecurity.config() != null) {
            for (ConfigMetadata config : oidcSecurity.config()) {
                configMetadataBuilder.put(config.key(), config.value());
            }
        }
        builder.addAttribute(OidcUtils.CONFIG_METADATA_ATTRIBUTE, new OidcConfigurationMetadata(configMetadataBuilder));

        return builder.build();
    }

    private String generateToken(jakarta.json.JsonObject claims) {
        try {
            return Jwt.claims(claims).sign(privateKey);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static OidcSecurity findOidcSecurity(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            if (ann instanceof OidcSecurity) {
                return (OidcSecurity) ann;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T convertClaimValue(Claim claim) {
        if (claim.type() != Object.class) {
            Converter<?> converter = converters.get(claim.type());
            if (converter != null) {
                return (T) converter.convert(claim.value());
            } else {
                throw new RuntimeException("Unsupported claim type: " + claim.type().getName());
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

    private static class JsonObjectConverter implements Converter<jakarta.json.JsonObject> {
        @Override
        public jakarta.json.JsonObject convert(String value) {
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
