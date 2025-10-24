package io.quarkus.proxy.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.proxy.config.ProxyConfig.NamedProxyConfig;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.proxy")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ProxyConfig {

    static final String NO_PROXY = "none";

    @WithParentName
    NamedProxyConfig defaultProxyConfig();

    @WithParentName
    Map<String, NamedProxyConfig> namedProxyConfigs();

    @ConfigGroup
    public interface NamedProxyConfig {

        /**
         * Proxy host.
         */
        Optional<String> host();

        /**
         * Proxy port
         */
        OptionalInt port();

        /**
         * The credential provider configuration for the proxy.
         * A credential provider offers a way to retrieve the proxy password.
         * Note that the credential provider is only used if the password is not set in the configuration.
         */
        ProxyCredentialProviderConfig credentialsProvider();

        /**
         * Proxy username.
         * <p>
         * See also {@code credentials-provider}
         */
        Optional<String> username();

        /**
         * Proxy password
         * <p>
         * See also {@code credentials-provider}
         */
        Optional<String> password();

        /**
         * Hostnames or IP addresses to exclude from proxying
         */
        Optional<List<String>> nonProxyHosts();

        /**
         * Proxy connection timeout.
         */
        @ConfigDocDefault("10s")
        Optional<Duration> proxyConnectTimeout();

        /**
         * The proxy type. Possible values are: {@code HTTP} (default), {@code SOCKS4} and {@code SOCKS5}.
         */
        @WithDefault("http")
        ProxyType type();

        /**
         * @return this {@link NamedProxyConfig} if {@link #type()} returns {@link ProxyType#HTTP}; otherwise throws an
         *         {@link IllegalStateException}
         * @throws IllegalStateException if {@link #type()} does not return {@link ProxyType#HTTP}
         */
        default NamedProxyConfig assertHttpType() {
            if (type() != ProxyType.HTTP) {
                throw new IllegalStateException("Proxy type HTTP is required");
            }
            return this;
        }

        static enum ProxyType {
            HTTP,
            SOCKS4,
            SOCKS5;
        }

    }

    @ConfigGroup
    interface ProxyCredentialProviderConfig {

        /**
         * The name of the "credential" bucket (map key -> passwords) to retrieve from the
         * {@link io.quarkus.credentials.CredentialsProvider}. If not set, the credential provider will not be used.
         * <p>
         * A credential provider offers a way to retrieve the key store password as well as alias password.
         * Note that the credential provider is only used if the passwords are not set in the configuration.
         */
        Optional<String> name();

        /**
         * The name of the bean providing the credential provider.
         * <p>
         * The name is used to select the credential provider to use.
         * The credential provider must be exposed as a CDI bean and with the {@code @Named} annotation set to the
         * configured name to be selected.
         * <p>
         * If not set, the default credential provider is used.
         */
        Optional<String> beanName();

        /**
         * The key used to retrieve the username from the "credential" bucket.
         * <p>
         * If username, password or both cannot be retrieved from the credential provider, then a RuntimeException is thrown.
         */
        @WithDefault(CredentialsProvider.USER_PROPERTY_NAME)
        String usernameKey();

        /**
         * The key used to retrieve the password from the "credential" bucket.
         * <p>
         * If username, password or both cannot be retrieved from the credential provider, then a RuntimeException is thrown.
         */
        @WithDefault(CredentialsProvider.PASSWORD_PROPERTY_NAME)
        String passwordKey();

    }
}
