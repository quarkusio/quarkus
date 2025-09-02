package io.quarkus.oidc.client.registration;

import static io.quarkus.jsonp.JsonProviderHolder.jsonProvider;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.jose4j.jwk.JsonWebKey.OutputControlLevel;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationException;
import io.quarkus.oidc.common.runtime.AbstractJsonObject;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;

public class ClientMetadata extends AbstractJsonObject {

    public ClientMetadata() {
        super();
    }

    public ClientMetadata(String json) {
        super(json);
    }

    public ClientMetadata(JsonObject json) {
        super(json);
    }

    public ClientMetadata(ClientMetadata metadata) {
        super(metadata.getJsonObject());
    }

    public String getClientId() {
        return super.getString(OidcConstants.CLIENT_ID);
    }

    public String getClientSecret() {
        return super.getString(OidcConstants.CLIENT_SECRET);
    }

    public String getClientName() {
        return super.getString(OidcConstants.CLIENT_METADATA_CLIENT_NAME);
    }

    public List<String> getRedirectUris() {
        return getListOfStrings(OidcConstants.CLIENT_METADATA_REDIRECT_URIS);
    }

    public List<String> getPostLogoutUris() {
        return getListOfStrings(OidcConstants.CLIENT_METADATA_POST_LOGOUT_URIS);
    }

    public String getMetadataString() {
        return super.getJsonString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ClientMetadata m) {
        return new Builder(m.getJsonObject());
    }

    public static class Builder {

        JsonObjectBuilder builder;
        boolean built = false;

        Builder() {
            builder = jsonProvider().createObjectBuilder();
        }

        Builder(JsonObject json) {
            builder = jsonProvider().createObjectBuilder(json);
        }

        public Builder clientName(String clientName) {
            if (built) {
                throw new IllegalStateException();
            }
            builder.add(OidcConstants.CLIENT_METADATA_CLIENT_NAME, clientName);
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            if (built) {
                throw new IllegalStateException();
            }
            builder.add(OidcConstants.CLIENT_METADATA_REDIRECT_URIS,
                    jsonProvider().createArrayBuilder().add(redirectUri).build());
            return this;
        }

        public Builder postLogoutUri(String postLogoutUri) {
            if (built) {
                throw new IllegalStateException();
            }
            builder.add(OidcConstants.CLIENT_METADATA_POST_LOGOUT_URIS,
                    jsonProvider().createArrayBuilder().add(postLogoutUri).build());
            return this;
        }

        public Builder grantType(String grantType) {
            return grantTypes(Set.of(grantType));
        }

        public Builder grantTypes(Set<String> grantTypes) {
            if (built) {
                throw new IllegalStateException();
            }
            JsonArrayBuilder arrayBuilder = jsonProvider().createArrayBuilder();
            for (String grantType : grantTypes) {
                arrayBuilder.add(grantType);
            }
            builder.add(OidcConstants.CLIENT_METADATA_GRANT_TYPES, arrayBuilder.build());
            return this;
        }

        public Builder tokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
            if (built) {
                throw new IllegalStateException();
            }
            builder.add(OidcConstants.CLIENT_METADATA_TOKEN_ENDPOINT_AUTH_METHOD, tokenEndpointAuthMethod);
            return this;
        }

        public Builder jwk(PublicKey publicKey) {
            return jwks(Set.of(publicKey));
        }

        public Builder jwks(Set<PublicKey> publicKeys) {
            if (built) {
                throw new IllegalStateException();
            }
            JsonArrayBuilder keysBuilder = jsonProvider().createArrayBuilder();
            for (PublicKey publicKey : publicKeys) {
                JsonObjectBuilder jwkBuilder = jsonProvider().createObjectBuilder();
                for (Map.Entry<String, Object> entry : convertPublicKeyToJwk(publicKey).entrySet()) {
                    jwkBuilder.add(entry.getKey(), entry.getValue().toString());
                }
                jwkBuilder.add("use", "sig");
                jwkBuilder.add("alg", getAlgorithm(publicKey));
                keysBuilder.add(jwkBuilder);
            }
            JsonObjectBuilder jwksBuilder = jsonProvider().createObjectBuilder();
            jwksBuilder.add("keys", keysBuilder);
            builder.add(OidcConstants.CLIENT_METADATA_JWKS, jwksBuilder);
            return this;
        }

        public Builder jwk(Map<String, String> keyProperties) {
            if (built) {
                throw new IllegalStateException();
            }
            JsonArrayBuilder keysBuilder = jsonProvider().createArrayBuilder();
            JsonObjectBuilder jwkBuilder = jsonProvider().createObjectBuilder();
            for (Map.Entry<String, String> entry : keyProperties.entrySet()) {
                jwkBuilder.add(entry.getKey(), entry.getValue());
            }
            keysBuilder.add(jwkBuilder);
            JsonObjectBuilder jwksBuilder = jsonProvider().createObjectBuilder();
            jwksBuilder.add("keys", keysBuilder);
            builder.add(OidcConstants.CLIENT_METADATA_JWKS, jwksBuilder);
            return this;
        }

        public Builder extraProps(Map<String, String> extraProps) {
            if (built) {
                throw new IllegalStateException();
            }
            builder.addAll(jsonProvider().createObjectBuilder(extraProps));
            return this;
        }

        private static Map<String, Object> convertPublicKeyToJwk(PublicKey key) {
            try {
                return PublicJsonWebKey.Factory.newPublicJwk(key).toParams(OutputControlLevel.PUBLIC_ONLY);
            } catch (JoseException ex) {
                throw new OidcClientRegistrationException(ex);
            }
        }

        private static String getAlgorithm(PublicKey publicKey) {
            if (publicKey instanceof RSAPublicKey) {
                return SignatureAlgorithm.RS256.getAlgorithm();
            } else if (publicKey instanceof ECPublicKey) {
                return SignatureAlgorithm.ES256.getAlgorithm();
            } else if (publicKey instanceof EdECPublicKey) {
                return SignatureAlgorithm.EDDSA.getAlgorithm();
            } else {
                throw new OidcClientRegistrationException("Unrecognized public key algorithm: " + publicKey.getAlgorithm());
            }
        }

        public ClientMetadata build() {
            built = true;
            return new ClientMetadata(builder.build());
        }
    }
}
