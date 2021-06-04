package io.quarkus.spring.cloud.config.client.runtime.credentials;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.runtime.OidcClientImpl;
import io.quarkus.oidc.client.runtime.TokensHelper;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.spring.cloud.config.client.runtime.SpringCloudClientCredentialsProvider;
import io.quarkus.spring.cloud.config.client.runtime.SpringCloudConfigClientConfig;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class SpringCloudConfigClientOidcCredentialsProvider implements SpringCloudClientCredentialsProvider {

    private static final String OIDC_CLIENT_ID = "SpringCloudConfigOidcClient";
    private static final String GRANT_TYPE = OidcClientConfig.Grant.Type.CLIENT.getGrantType();

    private final TokensHelper tokensHelper = new TokensHelper();
    private final OidcClient oidcClient;

    public SpringCloudConfigClientOidcCredentialsProvider(SpringCloudConfigClientConfig clientConfig, WebClient webClient) {
        OidcClientConfig oidcClientConfig = clientConfig.oidc;

        if (!oidcClientConfig.grant.getType().equals(OidcClientConfig.Grant.Type.CLIENT)) {
            throw new IllegalArgumentException("The only supported grant type is `client_credentials`.");
        }

        if (!oidcClientConfig.getId().isPresent()) {
            oidcClientConfig.setId(OIDC_CLIENT_ID);
        }

        String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcClientConfig);
        String tokenRequestUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcClientConfig.tokenPath);
        MultiMap grantClientParams = getGrantClientParams(oidcClientConfig);

        this.oidcClient = new OidcClientImpl(webClient, tokenRequestUri, GRANT_TYPE, grantClientParams, grantClientParams,
                oidcClientConfig);
        tokensHelper.initTokens(oidcClient);
    }

    @Override
    public void addAuthenticationInfo(HttpRequest<?> request) {
        request.bearerTokenAuthentication(tokensHelper.getTokens(oidcClient).await().indefinitely().getAccessToken());
    }

    private MultiMap getGrantClientParams(OidcClientConfig oidcConfig) {
        MultiMap grantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        grantParams.add(OidcConstants.GRANT_TYPE, GRANT_TYPE);
        grantParams.add(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
        grantParams.add(OidcConstants.CLIENT_SECRET, OidcCommonUtils.clientSecret(oidcConfig.getCredentials()));
        return grantParams;
    }
}
