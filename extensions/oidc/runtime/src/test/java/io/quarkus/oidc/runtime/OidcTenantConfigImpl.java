package io.quarkus.oidc.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

final class OidcTenantConfigImpl implements OidcTenantConfig {

    private final String tenantId;

    OidcTenantConfigImpl() {
        this.tenantId = null;
    }

    OidcTenantConfigImpl(String tenantId) {
        this.tenantId = tenantId;
    }

    enum ConfigMappingMethods {
        AUTH_SERVER_URL,
        DISCOVERY_ENABLED,
        REGISTRATION_PATH,
        CONNECTION_DELAY,
        CONNECTION_RETRY_COUNT,
        CONNECTION_TIMEOUT,
        USE_BLOCKING_DNS_LOOKUP,
        MAX_POOL_SIZE,
        FOLLOW_REDIRECTS,
        PROXY,
        PROXY_HOST,
        PROXY_PORT,
        PROXY_USERNAME,
        PROXY_PASSWORD,
        TLS,
        TLS_CONFIGURATION,
        TLS_VERIFICATION,
        TLS_KEYSTORE_FILE,
        TLS_KEYSTORE_FILE_TYPE,
        TLS_KEYSTORE_PROVIDER,
        TLS_KEYSTORE_PASSWORD,
        TLS_KEYSTORE_KEY_ALIAS,
        TLS_KEYSTORE_KEY_PASSWORD,
        TLS_TRUSTSTORE_PASSWORD,
        TLS_TRUSTSTORE_FILE,
        TLS_TRUSTSTORE_CERT_ALIAS,
        TLS_TRUSTSTORE_FILE_TYPE,
        TLS_TRUSTSTORE_PROVIDER,
        TOKEN_PATH,
        REVOKE_PATH,
        CLIENT_ID,
        CLIENT_NAME,
        CREDENTIALS,
        CREDENTIALS_SECRET,
        CREDENTIALS_CLIENT_SECRET,
        CREDENTIALS_CLIENT_SECRET_VALUE,
        CREDENTIALS_CLIENT_SECRET_PROVIDER,
        CREDENTIALS_CLIENT_SECRET_METHOD,
        CREDENTIALS_CLIENT_SECRET_PROVIDER_NAME,
        CREDENTIALS_CLIENT_SECRET_PROVIDER_KEYRING_NAME,
        CREDENTIALS_CLIENT_SECRET_PROVIDER_KEY,
        CREDENTIALS_JWT,
        CREDENTIALS_JWT_SOURCE,
        CREDENTIALS_JWT_SECRET,
        CREDENTIALS_JWT_SECRET_PROVIDER,
        CREDENTIALS_JWT_SECRET_PROVIDER_NAME,
        CREDENTIALS_JWT_SECRET_PROVIDER_KEYRING_NAME,
        CREDENTIALS_JWT_SECRET_PROVIDER_KEY,
        CREDENTIALS_JWT_KEY,
        CREDENTIALS_JWT_KEY_FILE,
        CREDENTIALS_JWT_KEY_STORE_FILE,
        CREDENTIALS_JWT_KEY_STORE_PASSWORD,
        CREDENTIALS_JWT_KEY_ID,
        CREDENTIALS_JWT_KEY_PASSWORD,
        CREDENTIALS_JWT_ISSUER,
        CREDENTIALS_JWT_SUBJECT,
        CREDENTIALS_JWT_CLAIMS,
        CREDENTIALS_JWT_SIGNATURE_ALGORITHM,
        CREDENTIALS_JWT_LIFESPAN,
        CREDENTIALS_JWT_ASSERTION,
        CREDENTIALS_JWT_AUDIENCE,
        CREDENTIALS_JWT_TOKEN_ID,
        PROVIDER,
        JWKS,
        CACHE_USER_INFO_ID_TOKEN,
        ALLOW_USER_INFO_CACHE,
        ALLOW_TOKEN_INTROSPECTION_CACHE,
        TOKEN_STATE_MANAGER,
        CODE_GRANT,
        AUTHENTICATION,
        CERTIFICATION_CHAIN,
        RESOURCE_METADATA,
        LOGOUT,
        TOKEN,
        ROLES,
        INTROSPECTION_CREDENTIALS,
        PUBLIC_KEY,
        TENANT_PATHS,
        END_SESSION_PATH,
        JWKS_PATH,
        INTROSPECTION_PATH,
        USER_INFO_PATH,
        AUTHORIZATION_PATH,
        APPLICATION_TYPE,
        TENANT_ENABLED,
        TOKEN_STATE_MANAGER_ENCRYPTION_ALGORITHM,
        TOKEN_STATE_MANAGER_ENCRYPTION_SECRET,
        TOKEN_STATE_MANAGER_ENCRYPTION_REQUIRED,
        TOKEN_STATE_MANAGER_SPLIT_TOKENS,
        TOKEN_STATE_MANAGER_STRATEGY,
        JWKS_RESOLVE_EARLY,
        JWKS_CACHE_SIZE,
        JWKS_CACHE_TIME_TO_LIVE,
        JWKS_CLEAN_UP_TIMER_INTERVAL,
        JWKS_TRY_ALL,
        CODE_GRANT_EXTRA_PARAMS,
        CODE_GRANT_HEADERS,
        AUTHENTICATION_RESPONSE_MODE,
        AUTHENTICATION_REDIRECT_PATH,
        AUTHENTICATION_RESTORE_PATH_AFTER_REDIRECT,
        AUTHENTICATION_REMOVE_REDIRECT_PARAMETERS,
        AUTHENTICATION_ERROR_PATH,
        AUTHENTICATION_SESSION_EXPIRED_PATH,
        AUTHENTICATION_VERIFY_ACCESS_TOKEN,
        AUTHENTICATION_FORCED_REDIRECT_HTTPS_SCHEME,
        AUTHENTICATION_SCOPES,
        AUTHENTICATION_SCOPE_SEPARATOR,
        AUTHENTICATION_NONCE_REQUIRED,
        AUTHENTICATION_ADD_OPENID_SCOPE,
        AUTHENTICATION_EXTRA_PARAMS,
        AUTHENTICATION_FORWARD_PARAMS,
        AUTHENTICATION_COOKIE_FORCE_SECURE,
        AUTHENTICATION_COOKIE_SUFFIX,
        AUTHENTICATION_COOKIE_PATH,
        AUTHENTICATION_COOKIE_PATH_HEADER,
        AUTHENTICATION_COOKIE_DOMAIN,
        AUTHENTICATION_COOKIE_SAME_SITE,
        AUTHENTICATION_ALLOW_MULTIPLE_CODE_FLOWS,
        AUTHENTICATION_FAIL_ON_MISSING_STATE_PARAM,
        AUTHENTICATION_FAIL_ON_UNRESOLVED_KID,
        AUTHENTICATION_USER_INFO_REQUIRED,
        AUTHENTICATION_SESSION_AGE_EXTENSION,
        AUTHENTICATION_STATE_COOKIE_AGE,
        AUTHENTICATION_JAVASCRIPT_AUTO_REDIRECT,
        AUTHENTICATION_ID_TOKEN_REQUIRED,
        AUTHENTICATION_INTERNAL_ID_TOKEN_LIFESPAN,
        AUTHENTICATION_PKCE_REQUIRED,
        AUTHENTICATION_PKCE_SECRET,
        AUTHENTICATION_STATE_SECRET,
        CERTIFICATION_CHAIN_LEAF_CERTIFICATE_NAME,
        CERTIFICATION_CHAIN_TRUST_STORE_FILE,
        CERTIFICATION_CHAIN_TRUST_STORE_PASSWORD,
        CERTIFICATION_CHAIN_TRUST_STORE_CERT_ALIAS,
        CERTIFICATION_CHAIN_TRUST_STORE_FILE_TYPE,
        RESOURCE_METADATA_ENABLED,
        RESOURCE_METADATA_RESOURCE,
        RESOURCE_METADATA_FORCE_HTTPS_SCHEME,
        LOGOUT_PATH,
        LOGOUT_POST_LOGOUT_PATH,
        LOGOUT_POST_LOGOUT_URI_PARAM,
        LOGOUT_CLEAR_SITE_DATA,
        LOGOUT_MODE,
        LOGOUT_EXTRA_PARAMS,
        LOGOUT_BACK_CHANNEL,
        LOGOUT_FRONT_CHANNEL,
        LOGOUT_FRONT_CHANNEL_PATH,
        LOGOUT_BACK_CHANNEL_PATH,
        LOGOUT_BACK_CHANNEL_TOKEN_CACHE_SIZE,
        LOGOUT_BACK_CHANNEL_TOKEN_CACHE_TTL,
        LOGOUT_BACK_CHANNEL_CLEAN_UP_TIMER_INTERVAL,
        LOGOUT_BACK_CHANNEL_LOGOUT_TOKEN_KEY,
        TOKEN_ISSUER,
        TOKEN_AUDIENCE,
        TOKEN_SUBJECT_REQUIRED,
        TOKEN_REQUIRED_CLAIMS,
        TOKEN_TOKEN_TYPE,
        TOKEN_LIFESPAN_GRACE,
        TOKEN_AGE,
        TOKEN_ISSUED_AT_REQUIRED,
        TOKEN_PRINCIPAL_CLAIM,
        TOKEN_REFRESH_EXPIRED,
        TOKEN_REFRESH_TOKEN_TIME_SKEW,
        TOKEN_FORCED_JWK_REFRESH_INTERNAL,
        TOKEN_HEADER,
        TOKEN_AUTHORIZATION_SCHEME,
        TOKEN_SIGNATURE_ALGORITHM,
        TOKEN_DECRYPTION_KEY_LOCATION,
        TOKEN_DECRYPT_ID_TOKEN,
        TOKEN_DECRYPT_ACCESS_TOKEN,
        TOKEN_ALLOW_JWT_INTROSPECTION,
        TOKEN_REQUIRE_JWT_INTROSPECTION_ONLY,
        TOKEN_ALLOW_OPAQUE_TOKEN_INTROSPECTION,
        TOKEN_CUSTOMIZER_NAME,
        TOKEN_VERIFY_ACCESS_TOKEN_WITH_USER_INFO,
        TOKEN_BINDING,
        TOKEN_BINDING_CERTIFICATE,
        ROLES_ROLE_CLAIM_PATH,
        ROLES_ROLE_CLAIM_SEPARATOR,
        ROLES_SOURCE,
        INTROSPECTION_CREDENTIALS_NAME,
        INTROSPECTION_CREDENTIALS_SECRET,
        INTROSPECTION_CREDENTIALS_INCLUDE_CLIENT_ID,
        TENANT_ID,
        JWT_BEARER_TOKEN_PATH
    }

    final Map<ConfigMappingMethods, Boolean> invocationsRecorder = new EnumMap<>(ConfigMappingMethods.class);

    @Override
    public Optional<String> tenantId() {
        invocationsRecorder.put(ConfigMappingMethods.TENANT_ID, true);
        return Optional.ofNullable(tenantId);
    }

    @Override
    public boolean tenantEnabled() {
        invocationsRecorder.put(ConfigMappingMethods.TENANT_ENABLED, true);
        return false;
    }

    @Override
    public Optional<ApplicationType> applicationType() {
        invocationsRecorder.put(ConfigMappingMethods.APPLICATION_TYPE, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> authorizationPath() {
        invocationsRecorder.put(ConfigMappingMethods.AUTHORIZATION_PATH, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> userInfoPath() {
        invocationsRecorder.put(ConfigMappingMethods.USER_INFO_PATH, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> introspectionPath() {
        invocationsRecorder.put(ConfigMappingMethods.INTROSPECTION_PATH, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> jwksPath() {
        invocationsRecorder.put(ConfigMappingMethods.JWKS_PATH, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> endSessionPath() {
        invocationsRecorder.put(ConfigMappingMethods.END_SESSION_PATH, true);
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> tenantPaths() {
        invocationsRecorder.put(ConfigMappingMethods.TENANT_PATHS, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> publicKey() {
        invocationsRecorder.put(ConfigMappingMethods.PUBLIC_KEY, true);
        return Optional.empty();
    }

    @Override
    public IntrospectionCredentials introspectionCredentials() {
        invocationsRecorder.put(ConfigMappingMethods.INTROSPECTION_CREDENTIALS, true);
        return new IntrospectionCredentials() {
            @Override
            public Optional<String> name() {
                invocationsRecorder.put(ConfigMappingMethods.INTROSPECTION_CREDENTIALS_NAME, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> secret() {
                invocationsRecorder.put(ConfigMappingMethods.INTROSPECTION_CREDENTIALS_SECRET, true);
                return Optional.empty();
            }

            @Override
            public boolean includeClientId() {
                invocationsRecorder.put(ConfigMappingMethods.INTROSPECTION_CREDENTIALS_INCLUDE_CLIENT_ID, true);
                return false;
            }
        };
    }

    @Override
    public Roles roles() {
        invocationsRecorder.put(ConfigMappingMethods.ROLES, true);
        return new Roles() {
            @Override
            public Optional<List<String>> roleClaimPath() {
                invocationsRecorder.put(ConfigMappingMethods.ROLES_ROLE_CLAIM_PATH, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> roleClaimSeparator() {
                invocationsRecorder.put(ConfigMappingMethods.ROLES_ROLE_CLAIM_SEPARATOR, true);
                return Optional.empty();
            }

            @Override
            public Optional<Source> source() {
                invocationsRecorder.put(ConfigMappingMethods.ROLES_SOURCE, true);
                return Optional.empty();
            }
        };
    }

    @Override
    public Token token() {
        invocationsRecorder.put(ConfigMappingMethods.TOKEN, true);
        return new Token() {
            @Override
            public Optional<String> issuer() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_ISSUER, true);
                return Optional.empty();
            }

            @Override
            public Optional<List<String>> audience() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_AUDIENCE, true);
                return Optional.empty();
            }

            @Override
            public boolean subjectRequired() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_SUBJECT_REQUIRED, true);
                return false;
            }

            @Override
            public Map<String, Set<String>> requiredClaims() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_REQUIRED_CLAIMS, true);
                return Map.of();
            }

            @Override
            public Optional<String> tokenType() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_TOKEN_TYPE, true);
                return Optional.empty();
            }

            @Override
            public OptionalInt lifespanGrace() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_LIFESPAN_GRACE, true);
                return OptionalInt.empty();
            }

            @Override
            public Optional<Duration> age() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_AGE, true);
                return Optional.empty();
            }

            @Override
            public boolean issuedAtRequired() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_ISSUED_AT_REQUIRED, true);
                return false;
            }

            @Override
            public Optional<String> principalClaim() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_PRINCIPAL_CLAIM, true);
                return Optional.empty();
            }

            @Override
            public boolean refreshExpired() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_REFRESH_EXPIRED, true);
                return false;
            }

            @Override
            public Optional<Duration> refreshTokenTimeSkew() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_REFRESH_TOKEN_TIME_SKEW, true);
                return Optional.empty();
            }

            @Override
            public Duration forcedJwkRefreshInterval() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_FORCED_JWK_REFRESH_INTERNAL, true);
                return null;
            }

            @Override
            public Optional<String> header() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_HEADER, true);
                return Optional.empty();
            }

            @Override
            public String authorizationScheme() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_AUTHORIZATION_SCHEME, true);
                return "";
            }

            @Override
            public Optional<SignatureAlgorithm> signatureAlgorithm() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_SIGNATURE_ALGORITHM, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> decryptionKeyLocation() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_DECRYPTION_KEY_LOCATION, true);
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> decryptIdToken() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_DECRYPT_ID_TOKEN, true);
                return Optional.of(false);
            }

            @Override
            public boolean decryptAccessToken() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_DECRYPT_ACCESS_TOKEN, true);
                return false;
            }

            @Override
            public boolean allowJwtIntrospection() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_ALLOW_JWT_INTROSPECTION, true);
                return false;
            }

            @Override
            public boolean requireJwtIntrospectionOnly() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_REQUIRE_JWT_INTROSPECTION_ONLY, true);
                return false;
            }

            @Override
            public boolean allowOpaqueTokenIntrospection() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_ALLOW_OPAQUE_TOKEN_INTROSPECTION, true);
                return false;
            }

            @Override
            public Optional<String> customizerName() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_CUSTOMIZER_NAME, true);
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> verifyAccessTokenWithUserInfo() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_VERIFY_ACCESS_TOKEN_WITH_USER_INFO, true);
                return Optional.empty();
            }

            @Override
            public Binding binding() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_BINDING, true);
                return new Binding() {
                    @Override
                    public boolean certificate() {
                        invocationsRecorder.put(ConfigMappingMethods.TOKEN_BINDING_CERTIFICATE, true);
                        return false;
                    }
                };
            }
        };
    }

    @Override
    public Logout logout() {
        invocationsRecorder.put(ConfigMappingMethods.LOGOUT, true);
        return new Logout() {
            @Override
            public Optional<String> path() {
                invocationsRecorder.put(ConfigMappingMethods.LOGOUT_PATH, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> postLogoutPath() {
                invocationsRecorder.put(ConfigMappingMethods.LOGOUT_POST_LOGOUT_PATH, true);
                return Optional.empty();
            }

            @Override
            public String postLogoutUriParam() {
                invocationsRecorder.put(ConfigMappingMethods.LOGOUT_POST_LOGOUT_URI_PARAM, true);
                return "";
            }

            @Override
            public Map<String, String> extraParams() {
                invocationsRecorder.put(ConfigMappingMethods.LOGOUT_EXTRA_PARAMS, true);
                return Map.of();
            }

            @Override
            public Optional<Set<ClearSiteData>> clearSiteData() {
                invocationsRecorder.put(ConfigMappingMethods.LOGOUT_CLEAR_SITE_DATA, true);
                return Optional.of(Set.of());
            }

            @Override
            public LogoutMode logoutMode() {
                invocationsRecorder.put(ConfigMappingMethods.LOGOUT_MODE, true);
                return LogoutMode.QUERY;
            }

            @Override
            public Backchannel backchannel() {
                invocationsRecorder.put(ConfigMappingMethods.LOGOUT_BACK_CHANNEL, true);
                return new Backchannel() {
                    @Override
                    public Optional<String> path() {
                        invocationsRecorder.put(ConfigMappingMethods.LOGOUT_BACK_CHANNEL_PATH, true);
                        return Optional.empty();
                    }

                    @Override
                    public int tokenCacheSize() {
                        invocationsRecorder.put(ConfigMappingMethods.LOGOUT_BACK_CHANNEL_TOKEN_CACHE_SIZE, true);
                        return 0;
                    }

                    @Override
                    public Duration tokenCacheTimeToLive() {
                        invocationsRecorder.put(ConfigMappingMethods.LOGOUT_BACK_CHANNEL_TOKEN_CACHE_TTL, true);
                        return null;
                    }

                    @Override
                    public Optional<Duration> cleanUpTimerInterval() {
                        invocationsRecorder.put(ConfigMappingMethods.LOGOUT_BACK_CHANNEL_CLEAN_UP_TIMER_INTERVAL,
                                true);
                        return Optional.empty();
                    }

                    @Override
                    public String logoutTokenKey() {
                        invocationsRecorder.put(ConfigMappingMethods.LOGOUT_BACK_CHANNEL_LOGOUT_TOKEN_KEY, true);
                        return "";
                    }
                };
            }

            @Override
            public Frontchannel frontchannel() {
                invocationsRecorder.put(ConfigMappingMethods.LOGOUT_FRONT_CHANNEL, true);
                return new Frontchannel() {
                    @Override
                    public Optional<String> path() {
                        invocationsRecorder.put(ConfigMappingMethods.LOGOUT_FRONT_CHANNEL_PATH, true);
                        return Optional.empty();
                    }
                };
            }
        };
    }

    @Override
    public CertificateChain certificateChain() {
        invocationsRecorder.put(ConfigMappingMethods.CERTIFICATION_CHAIN, true);
        return new CertificateChain() {
            @Override
            public Optional<String> leafCertificateName() {
                invocationsRecorder.put(ConfigMappingMethods.CERTIFICATION_CHAIN_LEAF_CERTIFICATE_NAME, true);
                return Optional.empty();
            }

            @Override
            public Optional<Path> trustStoreFile() {
                invocationsRecorder.put(ConfigMappingMethods.CERTIFICATION_CHAIN_TRUST_STORE_FILE, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStorePassword() {
                invocationsRecorder.put(ConfigMappingMethods.CERTIFICATION_CHAIN_TRUST_STORE_PASSWORD, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStoreCertAlias() {
                invocationsRecorder.put(ConfigMappingMethods.CERTIFICATION_CHAIN_TRUST_STORE_CERT_ALIAS, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStoreFileType() {
                invocationsRecorder.put(ConfigMappingMethods.CERTIFICATION_CHAIN_TRUST_STORE_FILE_TYPE, true);
                return Optional.empty();
            }
        };
    }

    @Override
    public ResourceMetadata resourceMetadata() {
        invocationsRecorder.put(ConfigMappingMethods.RESOURCE_METADATA, true);
        return new ResourceMetadata() {
            @Override
            public boolean enabled() {
                invocationsRecorder.put(ConfigMappingMethods.RESOURCE_METADATA_ENABLED, true);
                return false;
            }

            @Override
            public Optional<String> resource() {
                invocationsRecorder.put(ConfigMappingMethods.RESOURCE_METADATA_RESOURCE, true);
                return Optional.empty();
            }

            @Override
            public boolean forceHttpsScheme() {
                invocationsRecorder.put(ConfigMappingMethods.RESOURCE_METADATA_FORCE_HTTPS_SCHEME, true);
                return false;
            }
        };
    }

    @Override
    public Authentication authentication() {
        invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION, true);
        return new Authentication() {
            @Override
            public Optional<ResponseMode> responseMode() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_RESPONSE_MODE, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> redirectPath() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_REDIRECT_PATH, true);
                return Optional.empty();
            }

            @Override
            public boolean restorePathAfterRedirect() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_RESTORE_PATH_AFTER_REDIRECT, true);
                return false;
            }

            @Override
            public boolean removeRedirectParameters() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_REMOVE_REDIRECT_PARAMETERS, true);
                return false;
            }

            @Override
            public Optional<String> errorPath() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_ERROR_PATH, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> sessionExpiredPath() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_SESSION_EXPIRED_PATH, true);
                return Optional.empty();
            }

            @Override
            public boolean verifyAccessToken() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_VERIFY_ACCESS_TOKEN, true);
                return false;
            }

            @Override
            public Optional<Boolean> forceRedirectHttpsScheme() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_FORCED_REDIRECT_HTTPS_SCHEME, true);
                return Optional.empty();
            }

            @Override
            public Optional<List<String>> scopes() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_SCOPES, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> scopeSeparator() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_SCOPE_SEPARATOR, true);
                return Optional.empty();
            }

            @Override
            public boolean nonceRequired() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_NONCE_REQUIRED, true);
                return false;
            }

            @Override
            public Optional<Boolean> addOpenidScope() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_ADD_OPENID_SCOPE, true);
                return Optional.empty();
            }

            @Override
            public Map<String, String> extraParams() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_EXTRA_PARAMS, true);
                return Map.of();
            }

            @Override
            public Optional<List<@WithConverter(TrimmedStringConverter.class) String>> forwardParams() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_FORWARD_PARAMS, true);
                return Optional.empty();
            }

            @Override
            public boolean cookieForceSecure() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_COOKIE_FORCE_SECURE, true);
                return false;
            }

            @Override
            public Optional<String> cookieSuffix() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_COOKIE_SUFFIX, true);
                return Optional.empty();
            }

            @Override
            public String cookiePath() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_COOKIE_PATH, true);
                return "";
            }

            @Override
            public Optional<String> cookiePathHeader() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_COOKIE_PATH_HEADER, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> cookieDomain() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_COOKIE_DOMAIN, true);
                return Optional.empty();
            }

            @Override
            public CookieSameSite cookieSameSite() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_COOKIE_SAME_SITE, true);
                return CookieSameSite.LAX;
            }

            @Override
            public boolean allowMultipleCodeFlows() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_ALLOW_MULTIPLE_CODE_FLOWS, true);
                return false;
            }

            @Override
            public boolean failOnMissingStateParam() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_FAIL_ON_MISSING_STATE_PARAM, true);
                return false;
            }

            @Override
            public boolean failOnUnresolvedKid() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_FAIL_ON_UNRESOLVED_KID, true);
                return false;
            }

            @Override
            public Optional<Boolean> userInfoRequired() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_USER_INFO_REQUIRED, true);
                return Optional.empty();
            }

            @Override
            public Duration sessionAgeExtension() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_SESSION_AGE_EXTENSION, true);
                return null;
            }

            @Override
            public Duration stateCookieAge() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_STATE_COOKIE_AGE, true);
                return null;
            }

            @Override
            public boolean javaScriptAutoRedirect() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_JAVASCRIPT_AUTO_REDIRECT, true);
                return false;
            }

            @Override
            public Optional<Boolean> idTokenRequired() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_ID_TOKEN_REQUIRED, true);
                return Optional.empty();
            }

            @Override
            public Optional<Duration> internalIdTokenLifespan() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_INTERNAL_ID_TOKEN_LIFESPAN, true);
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> pkceRequired() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_PKCE_REQUIRED, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> pkceSecret() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_PKCE_SECRET, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> stateSecret() {
                invocationsRecorder.put(ConfigMappingMethods.AUTHENTICATION_STATE_SECRET, true);
                return Optional.empty();
            }
        };
    }

    @Override
    public CodeGrant codeGrant() {
        invocationsRecorder.put(ConfigMappingMethods.CODE_GRANT, true);
        return new CodeGrant() {
            @Override
            public Map<String, String> extraParams() {
                invocationsRecorder.put(ConfigMappingMethods.CODE_GRANT_EXTRA_PARAMS, true);
                return Map.of();
            }

            @Override
            public Map<String, String> headers() {
                invocationsRecorder.put(ConfigMappingMethods.CODE_GRANT_HEADERS, true);
                return Map.of();
            }
        };
    }

    @Override
    public TokenStateManager tokenStateManager() {
        invocationsRecorder.put(ConfigMappingMethods.TOKEN_STATE_MANAGER, true);
        return new TokenStateManager() {
            @Override
            public Strategy strategy() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_STATE_MANAGER_STRATEGY, true);
                return Strategy.ID_TOKEN;
            }

            @Override
            public boolean splitTokens() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_STATE_MANAGER_SPLIT_TOKENS, true);
                return false;
            }

            @Override
            public boolean encryptionRequired() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_STATE_MANAGER_ENCRYPTION_REQUIRED, true);
                return false;
            }

            @Override
            public Optional<String> encryptionSecret() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_STATE_MANAGER_ENCRYPTION_SECRET, true);
                return Optional.empty();
            }

            @Override
            public EncryptionAlgorithm encryptionAlgorithm() {
                invocationsRecorder.put(ConfigMappingMethods.TOKEN_STATE_MANAGER_ENCRYPTION_ALGORITHM, true);
                return EncryptionAlgorithm.A256GCMKW;
            }
        };
    }

    @Override
    public boolean allowTokenIntrospectionCache() {
        invocationsRecorder.put(ConfigMappingMethods.ALLOW_TOKEN_INTROSPECTION_CACHE, true);
        return false;
    }

    @Override
    public boolean allowUserInfoCache() {
        invocationsRecorder.put(ConfigMappingMethods.ALLOW_USER_INFO_CACHE, true);
        return false;
    }

    @Override
    public Optional<Boolean> cacheUserInfoInIdtoken() {
        invocationsRecorder.put(ConfigMappingMethods.CACHE_USER_INFO_ID_TOKEN, true);
        return Optional.empty();
    }

    @Override
    public Jwks jwks() {
        invocationsRecorder.put(ConfigMappingMethods.JWKS, true);
        return new Jwks() {
            @Override
            public boolean resolveEarly() {
                invocationsRecorder.put(ConfigMappingMethods.JWKS_RESOLVE_EARLY, true);
                return false;
            }

            @Override
            public int cacheSize() {
                invocationsRecorder.put(ConfigMappingMethods.JWKS_CACHE_SIZE, true);
                return 0;
            }

            @Override
            public Duration cacheTimeToLive() {
                invocationsRecorder.put(ConfigMappingMethods.JWKS_CACHE_TIME_TO_LIVE, true);
                return null;
            }

            @Override
            public Optional<Duration> cleanUpTimerInterval() {
                invocationsRecorder.put(ConfigMappingMethods.JWKS_CLEAN_UP_TIMER_INTERVAL, true);
                return Optional.empty();
            }

            @Override
            public boolean tryAll() {
                invocationsRecorder.put(ConfigMappingMethods.JWKS_TRY_ALL, true);
                return false;
            }
        };
    }

    @Override
    public Optional<Provider> provider() {
        invocationsRecorder.put(ConfigMappingMethods.PROVIDER, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> tokenPath() {
        invocationsRecorder.put(ConfigMappingMethods.TOKEN_PATH, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> revokePath() {
        invocationsRecorder.put(ConfigMappingMethods.REVOKE_PATH, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> clientId() {
        invocationsRecorder.put(ConfigMappingMethods.CLIENT_ID, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> clientName() {
        invocationsRecorder.put(ConfigMappingMethods.CLIENT_NAME, true);
        return Optional.empty();
    }

    @Override
    public Credentials credentials() {
        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS, true);
        return new Credentials() {
            @Override
            public Optional<String> secret() {
                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_SECRET, true);
                return Optional.empty();
            }

            @Override
            public Secret clientSecret() {
                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_CLIENT_SECRET, true);
                return new Secret() {

                    @Override
                    public Optional<String> value() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_CLIENT_SECRET_VALUE, true);
                        return Optional.empty();
                    }

                    @Override
                    public Provider provider() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_CLIENT_SECRET_PROVIDER, true);
                        return new Provider() {
                            @Override
                            public Optional<String> name() {
                                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_CLIENT_SECRET_PROVIDER_NAME, true);
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> keyringName() {
                                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_CLIENT_SECRET_PROVIDER_KEYRING_NAME,
                                        true);
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> key() {
                                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_CLIENT_SECRET_PROVIDER_KEY, true);
                                return Optional.empty();
                            }
                        };
                    }

                    @Override
                    public Optional<Method> method() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_CLIENT_SECRET_METHOD, true);
                        return Optional.empty();
                    }
                };
            }

            @Override
            public Jwt jwt() {
                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT, true);
                return new Jwt() {
                    @Override
                    public Source source() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_SOURCE, true);
                        return Source.BEARER;
                    }

                    @Override
                    public Optional<Path> tokenPath() {
                        invocationsRecorder.put(ConfigMappingMethods.JWT_BEARER_TOKEN_PATH, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> secret() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_SECRET, true);
                        return Optional.empty();
                    }

                    @Override
                    public Provider secretProvider() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_SECRET_PROVIDER, true);
                        return new Provider() {
                            @Override
                            public Optional<String> name() {
                                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_SECRET_PROVIDER_NAME, true);
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> keyringName() {
                                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_SECRET_PROVIDER_KEYRING_NAME,
                                        true);
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> key() {
                                invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_SECRET_PROVIDER_KEY, true);
                                return Optional.empty();
                            }
                        };
                    }

                    @Override
                    public Optional<String> key() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_KEY, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> keyFile() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_KEY_FILE, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> keyStoreFile() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_KEY_STORE_FILE, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> keyStorePassword() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_KEY_STORE_PASSWORD, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> keyId() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_KEY_ID, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> keyPassword() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_KEY_PASSWORD, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> audience() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_AUDIENCE, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> tokenKeyId() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_TOKEN_ID, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> issuer() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_ISSUER, true);
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> subject() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_SUBJECT, true);
                        return Optional.empty();
                    }

                    @Override
                    public Map<String, String> claims() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_CLAIMS, true);
                        return Map.of();
                    }

                    @Override
                    public Optional<String> signatureAlgorithm() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_SIGNATURE_ALGORITHM, true);
                        return Optional.empty();
                    }

                    @Override
                    public int lifespan() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_LIFESPAN, true);
                        return 0;
                    }

                    @Override
                    public boolean assertion() {
                        invocationsRecorder.put(ConfigMappingMethods.CREDENTIALS_JWT_ASSERTION, true);
                        return false;
                    }
                };
            }
        };
    }

    @Override
    public Optional<String> authServerUrl() {
        invocationsRecorder.put(ConfigMappingMethods.AUTH_SERVER_URL, true);
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> discoveryEnabled() {
        invocationsRecorder.put(ConfigMappingMethods.DISCOVERY_ENABLED, true);
        return Optional.empty();
    }

    @Override
    public Optional<String> registrationPath() {
        invocationsRecorder.put(ConfigMappingMethods.REGISTRATION_PATH, true);
        return Optional.empty();
    }

    @Override
    public Optional<Duration> connectionDelay() {
        invocationsRecorder.put(ConfigMappingMethods.CONNECTION_DELAY, true);
        return Optional.empty();
    }

    @Override
    public int connectionRetryCount() {
        invocationsRecorder.put(ConfigMappingMethods.CONNECTION_RETRY_COUNT, true);
        return 0;
    }

    @Override
    public Duration connectionTimeout() {
        invocationsRecorder.put(ConfigMappingMethods.CONNECTION_TIMEOUT, true);
        return null;
    }

    @Override
    public boolean useBlockingDnsLookup() {
        invocationsRecorder.put(ConfigMappingMethods.USE_BLOCKING_DNS_LOOKUP, true);
        return false;
    }

    @Override
    public OptionalInt maxPoolSize() {
        invocationsRecorder.put(ConfigMappingMethods.MAX_POOL_SIZE, true);
        return OptionalInt.empty();
    }

    @Override
    public boolean followRedirects() {
        invocationsRecorder.put(ConfigMappingMethods.FOLLOW_REDIRECTS, true);
        return false;
    }

    @Override
    public Proxy proxy() {
        invocationsRecorder.put(ConfigMappingMethods.PROXY, true);
        return new Proxy() {
            @Override
            public Optional<String> host() {
                invocationsRecorder.put(ConfigMappingMethods.PROXY_HOST, true);
                return Optional.empty();
            }

            @Override
            public int port() {
                invocationsRecorder.put(ConfigMappingMethods.PROXY_PORT, true);
                return 0;
            }

            @Override
            public Optional<String> username() {
                invocationsRecorder.put(ConfigMappingMethods.PROXY_USERNAME, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> password() {
                invocationsRecorder.put(ConfigMappingMethods.PROXY_PASSWORD, true);
                return Optional.empty();
            }
        };
    }

    @Override
    public Tls tls() {
        invocationsRecorder.put(ConfigMappingMethods.TLS, true);
        return new Tls() {
            @Override
            public Optional<String> tlsConfigurationName() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_CONFIGURATION, true);
                return Optional.empty();
            }

            @Override
            public Optional<Verification> verification() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_VERIFICATION, true);
                return Optional.empty();
            }

            @Override
            public Optional<Path> keyStoreFile() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_KEYSTORE_FILE, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> keyStoreFileType() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_KEYSTORE_FILE_TYPE, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> keyStoreProvider() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_KEYSTORE_PROVIDER, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> keyStorePassword() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_KEYSTORE_PASSWORD, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> keyStoreKeyAlias() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_KEYSTORE_KEY_ALIAS, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> keyStoreKeyPassword() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_KEYSTORE_KEY_PASSWORD, true);
                return Optional.empty();
            }

            @Override
            public Optional<Path> trustStoreFile() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_TRUSTSTORE_FILE, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStorePassword() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_TRUSTSTORE_PASSWORD, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStoreCertAlias() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_TRUSTSTORE_CERT_ALIAS, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStoreFileType() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_TRUSTSTORE_FILE_TYPE, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> trustStoreProvider() {
                invocationsRecorder.put(ConfigMappingMethods.TLS_TRUSTSTORE_PROVIDER, true);
                return Optional.empty();
            }
        };
    }
}
