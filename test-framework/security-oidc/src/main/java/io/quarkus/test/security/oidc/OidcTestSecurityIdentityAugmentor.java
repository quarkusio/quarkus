package io.quarkus.test.security.oidc;

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

    private static Map<String, ClaimType> standardClaimTypes = Map.of(
            Claims.exp.name(), ClaimType.LONG,
            Claims.iat.name(), ClaimType.LONG,
            Claims.nbf.name(), ClaimType.LONG,
            Claims.auth_time.name(), ClaimType.LONG,
            Claims.email_verified.name(), ClaimType.BOOLEAN);

    private static final PrivateKey privateKey;
    private Optional<String> issuer;

    static {
        try {
            privateKey = KeyUtils.generateKeyPair(2048).getPrivate();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public OidcTestSecurityIdentityAugmentor(Optional<String> issuer) {
        this.issuer = issuer;
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

    private Object convertClaimValue(Claim claim) {
        ClaimType type = claim.type();
        if (type == ClaimType.DEFAULT && standardClaimTypes.containsKey(claim.key())) {
            type = standardClaimTypes.get(claim.key());
        }
        return type.convert(claim.value());
    }
}
