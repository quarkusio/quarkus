package io.quarkus.oidc.client.registration;

import static io.quarkus.jsonp.JsonProviderHolder.jsonProvider;

import java.util.List;
import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import io.quarkus.oidc.common.runtime.AbstractJsonObject;
import io.quarkus.oidc.common.runtime.OidcConstants;

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

        public Builder extraProps(Map<String, String> extraProps) {
            if (built) {
                throw new IllegalStateException();
            }
            builder.addAll(jsonProvider().createObjectBuilder(extraProps));
            return this;
        }

        public ClientMetadata build() {
            built = true;
            return new ClientMetadata(builder.build());
        }
    }
}
