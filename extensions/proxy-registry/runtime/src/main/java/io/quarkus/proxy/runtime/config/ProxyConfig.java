package io.quarkus.proxy.runtime.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.proxy.ProxyType;
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

    @WithParentName
    NamedProxyConfig defaultProxyConfig();

    @WithParentName
    Map<String, NamedProxyConfig> namedProxyConfigs();

    @ConfigGroup
    interface NamedProxyConfig {

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
    }

    @ConfigGroup
    interface ProxyCredentialProviderConfig {

        /**
         * Name of the credential bucket to retrieve from the {@link CredentialsProvider}.
         * If not set, the credentials provider will not be used.
         * <p>
         * A credentials provider offers a way to retrieve the key store password as well as alias password.
         * Note that the credentials provider is only used if the username and password are not set in the configuration.
         */
        Optional<String> name();

        /**
         * Name of the bean providing the credentials provider.
         * <p>
         * The name is used to select the credentials provider to use.
         * The credentials provider must be exposed as a CDI bean and with the {@code @Named} annotation set
         * to the configured name to be selected.
         * <p>
         * If not set, the default credentials provider is used.
         */
        Optional<String> beanName();

        /**
         * The key used to retrieve the username from the credentials provider.
         * <p>
         * If username, password or both cannot be retrieved from the credentials provider, an exception is thrown.
         */
        @WithDefault(CredentialsProvider.USER_PROPERTY_NAME)
        String usernameKey();

        /**
         * The key used to retrieve the password from the credentials provider.
         * <p>
         * If username, password or both cannot be retrieved from the credentials provider, an exception is thrown.
         */
        @WithDefault(CredentialsProvider.PASSWORD_PROPERTY_NAME)
        String passwordKey();

    }
}
