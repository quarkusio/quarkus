package io.quarkus.oidc.client;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.oidc.client.runtime.OidcClientConfig;

final class OidcClientConfigImpl implements OidcClientConfig {

    enum ConfigMappingMethods {
        ID,
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
        HEADERS,
        EARLY_TOKENS_ACQUISITION,
        GRANT_OPTIONS,
        CLIENT_ENABLED,
        SCOPES,
        REFRESH_TOKEN_TIME_SKEW,
        ACCESS_TOKEN_EXPIRES_IN,
        ACCESS_TOKEN_EXPIRY_SKEW,
        ABSOLUTE_EXPIRES_IN,
        GRANT,
        GRANT_TYPE,
        GRANT_ACCESS_TOKEN_PROPERTY,
        GRANT_REFRESH_TOKEN_PROPERTY,
        GRANT_EXPIRES_IN_PROPERTY,
        REFRESH_EXPIRES_IN_PROPERTY,
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
        JWT_BEARER_TOKEN_PATH,
        REFRESH_INTERVAL
    }

    final Map<ConfigMappingMethods, Boolean> invocationsRecorder = new EnumMap<>(ConfigMappingMethods.class);

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
    public Optional<String> id() {
        invocationsRecorder.put(ConfigMappingMethods.ID, true);
        return Optional.empty();
    }

    @Override
    public boolean clientEnabled() {
        invocationsRecorder.put(ConfigMappingMethods.CLIENT_ENABLED, true);
        return false;
    }

    @Override
    public Optional<List<String>> scopes() {
        invocationsRecorder.put(ConfigMappingMethods.SCOPES, true);
        return Optional.empty();
    }

    @Override
    public Optional<Duration> refreshTokenTimeSkew() {
        invocationsRecorder.put(ConfigMappingMethods.REFRESH_TOKEN_TIME_SKEW, true);
        return Optional.empty();
    }

    @Override
    public Optional<Duration> accessTokenExpiresIn() {
        invocationsRecorder.put(ConfigMappingMethods.ACCESS_TOKEN_EXPIRES_IN, true);
        return Optional.empty();
    }

    @Override
    public Optional<Duration> accessTokenExpirySkew() {
        invocationsRecorder.put(ConfigMappingMethods.ACCESS_TOKEN_EXPIRY_SKEW, true);
        return Optional.empty();
    }

    @Override
    public boolean absoluteExpiresIn() {
        invocationsRecorder.put(ConfigMappingMethods.ABSOLUTE_EXPIRES_IN, true);
        return false;
    }

    @Override
    public Grant grant() {
        invocationsRecorder.put(ConfigMappingMethods.GRANT, true);
        return new Grant() {
            @Override
            public Type type() {
                invocationsRecorder.put(ConfigMappingMethods.GRANT_TYPE, true);
                return Type.CLIENT;
            }

            @Override
            public String accessTokenProperty() {
                invocationsRecorder.put(ConfigMappingMethods.GRANT_ACCESS_TOKEN_PROPERTY, true);
                return "";
            }

            @Override
            public String refreshTokenProperty() {
                invocationsRecorder.put(ConfigMappingMethods.GRANT_REFRESH_TOKEN_PROPERTY, true);
                return "";
            }

            @Override
            public String expiresInProperty() {
                invocationsRecorder.put(ConfigMappingMethods.GRANT_EXPIRES_IN_PROPERTY, true);
                return "";
            }

            @Override
            public String refreshExpiresInProperty() {
                invocationsRecorder.put(ConfigMappingMethods.REFRESH_EXPIRES_IN_PROPERTY, true);
                return "";
            }
        };
    }

    @Override
    public Map<String, Map<String, String>> grantOptions() {
        invocationsRecorder.put(ConfigMappingMethods.GRANT_OPTIONS, true);
        return Map.of();
    }

    @Override
    public boolean earlyTokensAcquisition() {
        invocationsRecorder.put(ConfigMappingMethods.EARLY_TOKENS_ACQUISITION, true);
        return false;
    }

    @Override
    public Map<String, String> headers() {
        invocationsRecorder.put(ConfigMappingMethods.HEADERS, true);
        return Map.of();
    }

    @Override
    public Optional<Duration> refreshInterval() {
        invocationsRecorder.put(ConfigMappingMethods.REFRESH_INTERVAL, true);
        return Optional.empty();
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
