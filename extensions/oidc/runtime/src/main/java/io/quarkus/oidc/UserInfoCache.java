package io.quarkus.oidc;

import io.smallrye.mutiny.Uni;

/**
 * UserInfo cache.
 */
public interface UserInfoCache {

    /**
     * Add a new {@link UserInfo} to the cache.
     *
     * @param token the token which was used to get {@link UserInfo}
     * @param userInfo {@link UserInfo}
     * @param oidcConfig the tenant configuration
     * @param requestContext the request context which can be used to run the blocking tasks
     */
    Uni<Void> addUserInfo(String token, UserInfo userInfo, OidcTenantConfig oidcConfig,
            OidcRequestContext<Void> requestContext);

    /**
     * Get the cached {@link UserInfo}.
     *
     * @param token the token which will be used to get new {@link UserInfo} if no {@link UserInfo} is cached.
     *        Effectively this token is a cache key which has to be stored when
     *        {@link #addUserInfo(String, UserInfo, OidcTenantConfig, AddUserInfoRequestContext)}
     *        is called.
     * @param oidcConfig the tenant configuration
     * @param requestContext the request context which can be used to run the blocking tasks
     */
    Uni<UserInfo> getUserInfo(String token, OidcTenantConfig oidcConfig, OidcRequestContext<UserInfo> requestContext);
}
