package io.quarkus.test.security.oidc;

import java.lang.annotation.Annotation;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;

import io.quarkus.arc.Unremovable;
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

@ApplicationScoped
public class OidcTestSecurityIdentityAugmentorProducer {

    @Inject
    @ConfigProperty(name = "quarkus.oidc.token.issuer")
    Optional<String> issuer;

    PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            privateKey = KeyUtils.generateKeyPair(2048).getPrivate();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Produces
    @Unremovable
    public TestSecurityIdentityAugmentor produce() {
        return new OidcTestSecurityIdentityAugmentor();
    }

    private class OidcTestSecurityIdentityAugmentor implements TestSecurityIdentityAugmentor {

        @Override
        public SecurityIdentity augment(final SecurityIdentity identity, final Annotation[] annotations) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

            final OidcSecurity oidcSecurity = findOidcSecurity(annotations);

            final boolean introspectionRequired = oidcSecurity != null && oidcSecurity.introspectionRequired();

            if (!introspectionRequired) {
                // JsonWebToken
                JwtClaims claims = new JwtClaims();
                claims.setClaim(Claims.preferred_username.name(), identity.getPrincipal().getName());
                claims.setClaim(Claims.groups.name(), identity.getRoles().stream().collect(Collectors.toList()));
                if (oidcSecurity != null && oidcSecurity.claims() != null) {
                    for (Claim claim : oidcSecurity.claims()) {
                        claims.setClaim(claim.key(), claim.value());
                    }
                }
                String jwt = generateToken(claims);
                IdTokenCredential idToken = new IdTokenCredential(jwt, null);
                AccessTokenCredential accessToken = new AccessTokenCredential(jwt, null);

                JsonWebToken principal = new OidcJwtCallerPrincipal(claims, idToken);
                builder.setPrincipal(principal);
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

        private String generateToken(JwtClaims claims) {
            try {
                return Jwt.claims(claims.getClaimsMap()).sign(privateKey);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private OidcSecurity findOidcSecurity(Annotation[] annotations) {
            for (Annotation ann : annotations) {
                if (ann instanceof OidcSecurity) {
                    return (OidcSecurity) ann;
                }
            }
            return null;
        }
    }

}
