package io.quarkus.oidc.redis.token.state.manager.runtime;

import io.quarkus.oidc.AuthorizationCodeTokens;

public record AuthorizationCodeTokensRecord(String idToken, String accessToken, String refreshToken, Long accessTokenExpiresIn,
        String accessTokenScope) {

    static AuthorizationCodeTokensRecord of(AuthorizationCodeTokens tokens) {
        return new AuthorizationCodeTokensRecord(tokens.getIdToken(), tokens.getAccessToken(), tokens.getRefreshToken(),
                tokens.getAccessTokenExpiresIn(), tokens.getAccessTokenScope());
    }

    AuthorizationCodeTokens toTokens() {
        return new AuthorizationCodeTokens(idToken, accessToken, refreshToken, accessTokenExpiresIn, accessTokenScope);
    }
}
