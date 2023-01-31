package io.quarkus.proxy.common;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import io.netty.handler.ssl.ClientAuth;

/**
 * SSL client configuration holder/validator
 */
public interface Ssl {
    Optional<URL> keyUrl();

    Optional<URL> keyCertChainUrl();

    Optional<URL> trustCertCollectionUrl();

    /**
     * Validate configuration and return whether SSL is enabled.
     *
     * @param ssl the SSL configuration to validate
     * @return {@code true} if SSL is enabled
     */
    static boolean isEnabled(Ssl ssl) {
        if (ssl.keyUrl().isPresent() != ssl.keyCertChainUrl().isPresent()) {
            throw new IllegalArgumentException("Either both of `keyUrl` and `keyCertChainUrl` should be set or none");
        }
        return ssl.keyUrl().isPresent();
    }

    static Ssl create(
        URL keyUrl,
        URL keyCertChainUrl
    ) {
        return new Impl(Optional.of(keyUrl), Optional.of(keyCertChainUrl), Optional.empty());
    }

    static Ssl create(
        URL keyUrl,
        URL keyCertChainUrl,
        URL trustCertCollectionUrl
    ) {
        return new Impl(Optional.of(keyUrl), Optional.of(keyCertChainUrl), Optional.of(trustCertCollectionUrl));
    }

    class Impl implements Ssl {
        private final Optional<URL> keyUrl;
        private final Optional<URL> keyCertChainUrl;
        private final Optional<URL> trustCertCollectionUrl;

        private Impl(Ssl ssl) {
            this(
                ssl.keyUrl(),
                ssl.keyCertChainUrl(),
                ssl.trustCertCollectionUrl()
            );
        }

        private Impl(Optional<URL> keyUrl, Optional<URL> keyCertChainUrl, Optional<URL> trustCertCollectionUrl) {
            this.keyUrl = Objects.requireNonNull(keyUrl, "keyUrl");
            this.keyCertChainUrl = Objects.requireNonNull(keyCertChainUrl, "keyCertChainUrl");
            this.trustCertCollectionUrl = Objects.requireNonNull(trustCertCollectionUrl, "trustCertCollectionUrl");
        }

        @Override
        public Optional<URL> keyUrl() {
            return keyUrl;
        }

        @Override
        public Optional<URL> keyCertChainUrl() {
            return keyCertChainUrl;
        }

        @Override
        public Optional<URL> trustCertCollectionUrl() {
            return trustCertCollectionUrl;
        }
    }

    /**
     * SSL server configuration holder/validator
     */
    interface Server extends Ssl {
        Optional<ClientAuth> clientAuth();

        static Server create(
            URL keyUrl,
            URL keyCertChainUrl
        ) {
            return new Server.Impl(Optional.of(keyUrl), Optional.of(keyCertChainUrl), Optional.empty(), Optional.empty());
        }

        static Server create(
            URL keyUrl,
            URL keyCertChainUrl,
            URL trustCertCollectionUrl,
            ClientAuth clientAuth
        ) {
            return new Server.Impl(Optional.of(keyUrl), Optional.of(keyCertChainUrl), Optional.of(trustCertCollectionUrl), Optional.of(clientAuth));
        }

        class Impl extends Ssl.Impl implements Server {
            private final Optional<ClientAuth> clientAuth;

            private Impl(Ssl.Server ssl) {
                this(
                    ssl.keyUrl(),
                    ssl.keyCertChainUrl(),
                    ssl.trustCertCollectionUrl(),
                    ssl.clientAuth()
                );
            }

            public Impl(Optional<URL> keyUrl, Optional<URL> keyCertChainUrl, Optional<URL> trustCertCollectionUrl, Optional<ClientAuth> clientAuth) {
                super(keyUrl, keyCertChainUrl, trustCertCollectionUrl);
                this.clientAuth = Objects.requireNonNull(clientAuth, "clientAuth");
            }

            @Override
            public Optional<ClientAuth> clientAuth() {
                return clientAuth;
            }
        }
    }
}
