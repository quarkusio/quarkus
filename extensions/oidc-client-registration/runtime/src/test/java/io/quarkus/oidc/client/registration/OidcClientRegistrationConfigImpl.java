package io.quarkus.oidc.client.registration;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationConfig;

final class OidcClientRegistrationConfigImpl implements OidcClientRegistrationConfig {

    enum ConfigMappingMethods {
        ID,
        REGISTRATION_ENABLED,
        REGISTER_EARLY,
        INITIAL_TOKEN,
        METADATA,
        METADATA_CLIENT_NAME,
        METADATA_REDIRECT_URI,
        METADATA_POST_LOGOUT_URI,
        METADATA_EXTRA_PROPS,
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
        TLS_TRUSTSTORE_PROVIDER
    }

    final Map<ConfigMappingMethods, Boolean> invocationsRecorder = new HashMap<>();

    @Override
    public Optional<String> id() {
        invocationsRecorder.put(ConfigMappingMethods.ID, true);
        return Optional.empty();
    }

    @Override
    public boolean registrationEnabled() {
        invocationsRecorder.put(ConfigMappingMethods.REGISTRATION_ENABLED, true);
        return false;
    }

    @Override
    public boolean registerEarly() {
        invocationsRecorder.put(ConfigMappingMethods.REGISTER_EARLY, true);
        return false;
    }

    @Override
    public Optional<String> initialToken() {
        invocationsRecorder.put(ConfigMappingMethods.INITIAL_TOKEN, true);
        return Optional.empty();
    }

    @Override
    public Metadata metadata() {
        invocationsRecorder.put(ConfigMappingMethods.METADATA, true);
        return new Metadata() {
            @Override
            public Optional<String> clientName() {
                invocationsRecorder.put(ConfigMappingMethods.METADATA_CLIENT_NAME, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> redirectUri() {
                invocationsRecorder.put(ConfigMappingMethods.METADATA_REDIRECT_URI, true);
                return Optional.empty();
            }

            @Override
            public Optional<String> postLogoutUri() {
                invocationsRecorder.put(ConfigMappingMethods.METADATA_POST_LOGOUT_URI, true);
                return Optional.empty();
            }

            @Override
            public Map<String, String> extraProps() {
                invocationsRecorder.put(ConfigMappingMethods.METADATA_EXTRA_PROPS, true);
                return Map.of();
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
