package io.quarkus.proxy.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.proxy.runtime.config.ProxyConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ProxyConfigurationRecorder {
    private final RuntimeValue<ProxyConfig> runtimeConfig;

    public ProxyConfigurationRecorder(RuntimeValue<ProxyConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public Supplier<ProxyConfigurationRegistry> init() {
        ProxyConfig proxyConfig = runtimeConfig.getValue();

        Optional<ProxyConfiguration> defaultConfig = build("quarkus.proxy.", proxyConfig.defaultProxyConfig());

        Map<String, ProxyConfiguration> namedConfigs = new HashMap<>();
        for (Map.Entry<String, ProxyConfig.NamedProxyConfig> entry : proxyConfig.namedProxyConfigs().entrySet()) {
            String name = entry.getKey();
            if (ProxyConfigurationRegistry.NONE.equals(name)) {
                throw new IllegalStateException("Proxy configuration name `none` has a special meaning and configuring it"
                        + " via quarkus.proxy.\"none\".* options is not possible. Remove all quarkus.proxy.\"none\".* keys"
                        + " from your configuration.");
            }

            Optional<ProxyConfiguration> namedConfig = build("quarkus.proxy.\"" + name + "\".", entry.getValue());
            if (namedConfig.isPresent()) {
                namedConfigs.put(name, namedConfig.get());
            }
        }

        ProxyConfigurationRegistry registry = new ProxyConfigurationRegistryImpl(namedConfigs, defaultConfig);

        return new Supplier<ProxyConfigurationRegistry>() {
            @Override
            public ProxyConfigurationRegistry get() {
                return registry;
            }
        };
    }

    // `prefix` is only used in error messages
    private Optional<ProxyConfiguration> build(String prefix, ProxyConfig.NamedProxyConfig config) {
        if (config.host().isEmpty()) {
            StringJoiner badValues = new StringJoiner(", ");
            if (config.port().isPresent()) {
                badValues.add("port");
            }
            if (config.credentialsProvider().name().isPresent()) {
                badValues.add("credentials-provider");
            }
            if (config.username().isPresent()) {
                badValues.add("username");
            }
            if (config.password().isPresent()) {
                badValues.add("password");
            }
            if (config.nonProxyHosts().isPresent()) {
                badValues.add("non-proxy-hosts");
            }
            if (config.proxyConnectTimeout().isPresent()) {
                badValues.add("proxy-connect-timeout");
            }
            if (badValues.length() > 0) {
                throw new IllegalStateException(
                        "If " + prefix + ".host is not set, then all of " + badValues + " must not be set either");
            }
            return Optional.empty();
        }

        if (config.port().isEmpty()) {
            throw new IllegalStateException("If " + prefix + ".host is set, " + prefix + ".port must also be set");
        }

        if (config.username().isPresent() != config.password().isPresent()) {
            throw new IllegalStateException(prefix + ".username and " + prefix + ".password must be both set or both unset");
        }

        Optional<String> username;
        Optional<String> password;
        if (config.username().isPresent() && config.password().isPresent()) {
            username = config.username();
            password = config.password();
        } else {
            ProxyConfig.ProxyCredentialProviderConfig providerConfig = config.credentialsProvider();
            if (providerConfig.name().isPresent()) {
                CredentialsProvider provider = CredentialsProviderFinder.find(providerConfig.beanName().orElse(null));
                Map<String, String> credentials = provider.getCredentialsAsync(providerConfig.name().get())
                        .await().indefinitely();
                username = Optional.ofNullable(credentials.get(providerConfig.usernameKey()));
                password = Optional.ofNullable(credentials.get(providerConfig.passwordKey()));
                if (username.isEmpty() || password.isEmpty()) {
                    StringJoiner missingKeys = new StringJoiner(" and ");
                    if (username.isEmpty()) {
                        missingKeys.add(providerConfig.usernameKey());
                    }
                    if (password.isEmpty()) {
                        missingKeys.add(providerConfig.passwordKey());
                    }
                    throw new IllegalStateException("Could not retrieve " + missingKeys + " from credentials provider "
                            + providerConfig.name().get());
                }
            } else {
                username = Optional.empty();
                password = Optional.empty();
            }
        }

        return Optional.of(new ProxyConfigurationImpl(
                config.host().get(),
                config.port().getAsInt(),
                username,
                password,
                config.nonProxyHosts(),
                config.proxyConnectTimeout(),
                config.type()));
    }
}
