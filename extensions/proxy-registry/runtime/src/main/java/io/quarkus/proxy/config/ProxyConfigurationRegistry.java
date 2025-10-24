package io.quarkus.proxy.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.literal.NamedLiteral;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.proxy.config.ProxyConfig.NamedProxyConfig;
import io.quarkus.proxy.config.ProxyConfig.ProxyCredentialProviderConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ProxyConfigurationRegistry {

    private Map<String, Optional<NamedProxyConfig>> namedProxyConfigs;
    private Optional<NamedProxyConfig> defaultProxyConfig;

    public Optional<NamedProxyConfig> getProxyConfig(Optional<String> name, NoneReturnValue noneBehavior) {
        if (name.isEmpty()) {
            return defaultProxyConfig;
        }
        final String plainName = name.get();
        if (ProxyConfig.NO_PROXY.equals(plainName)) {
            switch (noneBehavior) {
                case NONE_INSTANCE: {
                    return None.INSTANCE;
                }
                case EMPTY: {
                    return Optional.empty();
                }
                default:
                    throw new IllegalArgumentException("Unexpected value: " + noneBehavior);
            }
        }
        final Optional<NamedProxyConfig> namedProxyConfig = namedProxyConfigs.getOrDefault(plainName, Optional.empty());
        if (namedProxyConfig == null) {
            throw new IllegalStateException("Proxy configuration with name " + plainName + " was requested but "
                    + "quarkus.proxy.\"" + plainName + "\".host is not defined");
        }
        return namedProxyConfig;
    }

    public static Optional<UsernamePassword> getUsernamePassword(NamedProxyConfig config) {
        if (config.username().isPresent() && config.password().isPresent()) {
            return Optional.of(new UsernamePassword(config.username().get(), config.password().get()));
        } else {
            final ProxyCredentialProviderConfig cpConfig = config.credentialsProvider();
            if (cpConfig.name().isPresent()) {
                CredentialsProvider provider = lookupCredentialsProvider(cpConfig.beanName().orElse(null));
                Map<String, String> creds = provider.getCredentialsAsync(cpConfig.name().get()).await().indefinitely();
                String username = creds.get(cpConfig.usernameKey());
                String password = creds.get(cpConfig.passwordKey());
                if (username == null || password == null) {
                    final String badKeys = Map.of(cpConfig.usernameKey(), username, cpConfig.passwordKey(), password).entrySet()
                            .stream()
                            .filter(en -> en.getValue() == null)
                            .map(Entry::getKey)
                            .collect(Collectors.joining(" and "));
                    throw new IllegalStateException(
                            "Could not retrieve " + badKeys + " from credential bucket " + cpConfig.name().get());
                }
                return Optional.of(new UsernamePassword(username, password));
            }
        }
        return Optional.empty();
    }

    static CredentialsProvider lookupCredentialsProvider(String name) {
        ArcContainer container = Arc.container();
        InstanceHandle<CredentialsProvider> instance;
        if (name == null) {
            instance = container.instance(CredentialsProvider.class);
        } else {
            instance = container.instance(CredentialsProvider.class, NamedLiteral.of(name));
        }

        if (!instance.isAvailable()) {
            if (name == null) {
                throw new RuntimeException("Unable to find the default credentials provider");
            } else {
                throw new RuntimeException("Unable to find the credentials provider named '" + name + "'");
            }
        }
        return instance.get();
    }

    static Optional<NamedProxyConfig> assertValid(String prefix, NamedProxyConfig config) {
        if (config.host().isEmpty()) {
            String badValues = Map.<String, Optional<?>> of(
                    "port", config.port().isPresent() ? Optional.of(config.port().getAsInt()) : Optional.empty(),
                    "username", config.username(),
                    "password", config.password(),
                    "nonProxyHosts", config.nonProxyHosts(),
                    "proxyConnectTimeout", config.proxyConnectTimeout())
                    .entrySet().stream()
                    .filter(en -> en.getValue().isPresent())
                    .map(en -> prefix + en.getKey())
                    .collect(Collectors.joining(", "));
            if (!badValues.isEmpty()) {
                throw new IllegalStateException(
                        "If " + prefix + ".host is not set, then all of " + badValues + " must not be set too");
            }
            return Optional.empty();
        } else {
            /* host is present */
            if (config.port().isEmpty()) {
                throw new IllegalStateException("If " + prefix + ".host is set then " + prefix + ".port must be set too");
            }

            if (config.username().isPresent() != config.password().isPresent()) {
                throw new IllegalStateException(
                        prefix + ".username and " + prefix + ".password must be both set or both left unset");
            }
            return Optional.of(config);
        }
    }

    public void init(ProxyConfig proxyConfig) {
        final HashMap<String, Optional<NamedProxyConfig>> m = new HashMap<>();
        for (Entry<String, NamedProxyConfig> en : proxyConfig.namedProxyConfigs().entrySet()) {
            final String name = en.getKey();
            if (ProxyConfig.NO_PROXY.equals(name)) {
                throw new IllegalStateException(
                        "Proxy configuration name `none` has a special meaning and configuring it via quarkus.proxy.\"none\".* options is not possible. Remove all quarkus.proxy.\"none\".* keys from your configuration.");
            }
            Optional<NamedProxyConfig> validated = assertValid("quarkus.proxy.\"" + name + "\".", en.getValue());
            if (validated.isPresent()) {
                m.put(name, validated);
            }
        }
        this.namedProxyConfigs = m;
        this.defaultProxyConfig = assertValid("quarkus.proxy.",
                proxyConfig.defaultProxyConfig());

    }

    public Supplier<ProxyConfigurationRegistry> getSupplier() {
        return new Supplier<ProxyConfigurationRegistry>() {
            @Override
            public ProxyConfigurationRegistry get() {
                return ProxyConfigurationRegistry.this;
            }
        };
    }

    public enum NoneReturnValue {
        EMPTY,
        NONE_INSTANCE;
    }

    public static class UsernamePassword {
        private final String username;
        private final String password;

        public UsernamePassword(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    static class None implements NamedProxyConfig {
        static final Optional<NamedProxyConfig> INSTANCE = Optional.of(new None());
        static final OptionalInt ZERO_OPTIONAL = OptionalInt.of(0);
        static final Optional<String> NO_PROXY_OPTIONAL = Optional.of(ProxyConfig.NO_PROXY);

        @Override
        public Optional<String> host() {
            return NO_PROXY_OPTIONAL;
        }

        @Override
        public OptionalInt port() {
            return ZERO_OPTIONAL;
        }

        @Override
        public Optional<String> username() {
            return Optional.empty();
        }

        @Override
        public Optional<String> password() {
            return Optional.empty();
        }

        @Override
        public Optional<List<String>> nonProxyHosts() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> proxyConnectTimeout() {
            return Optional.empty();
        }

        @Override
        public NamedProxyConfig.ProxyType type() {
            return NamedProxyConfig.ProxyType.HTTP;
        }

        @Override
        public ProxyCredentialProviderConfig credentialsProvider() {
            return NoProxyCredentialProviderConfig.INSTANCE;
        }

    }

    static class NoProxyCredentialProviderConfig implements ProxyCredentialProviderConfig {
        static final ProxyCredentialProviderConfig INSTANCE = new NoProxyCredentialProviderConfig();

        @Override
        public Optional<String> name() {
            return Optional.empty();
        }

        @Override
        public Optional<String> beanName() {
            return Optional.empty();
        }

        @Override
        public String usernameKey() {
            return CredentialsProvider.USER_PROPERTY_NAME;
        }

        @Override
        public String passwordKey() {
            return CredentialsProvider.PASSWORD_PROPERTY_NAME;
        }

    }
}
