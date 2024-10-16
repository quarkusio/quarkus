package io.quarkus.keycloak.adminclient;

import static io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfigUtil.validate;

import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.spi.ResteasyClientProvider;

import io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfig;
import io.quarkus.resteasy.common.runtime.jackson.QuarkusJacksonSerializer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

@Recorder
public class ResteasyKeycloakAdminClientRecorder {

    private final RuntimeValue<KeycloakAdminClientConfig> keycloakAdminClientConfigRuntimeValue;

    public ResteasyKeycloakAdminClientRecorder(
            RuntimeValue<KeycloakAdminClientConfig> keycloakAdminClientConfigRuntimeValue) {
        this.keycloakAdminClientConfigRuntimeValue = keycloakAdminClientConfigRuntimeValue;
    }

    public Supplier<Keycloak> createAdminClient() {

        final KeycloakAdminClientConfig config = keycloakAdminClientConfigRuntimeValue.getValue();
        validate(config);
        if (config.serverUrl().isEmpty()) {
            return new Supplier<>() {
                @Override
                public Keycloak get() {
                    throw new IllegalStateException(
                            "'quarkus.keycloak.admin-client.server-url' must be set in order to use the Keycloak admin client as a CDI bean");
                }
            };
        }
        final KeycloakBuilder keycloakBuilder = KeycloakBuilder
                .builder()
                .clientId(config.clientId())
                .clientSecret(config.clientSecret().orElse(null))
                .grantType(config.grantType().asString())
                .username(config.username().orElse(null))
                .password(config.password().orElse(null))
                .realm(config.realm())
                .serverUrl(config.serverUrl().get())
                .scope(config.scope().orElse(null));
        return new Supplier<Keycloak>() {
            @Override
            public Keycloak get() {
                return keycloakBuilder.build();
            }
        };
    }

    public void setClientProvider(boolean areJSONBProvidersPresent, Supplier<TlsConfigurationRegistry> registrySupplier) {
        var registry = registrySupplier.get();
        var namedTlsConfig = TlsConfiguration.from(registry,
                keycloakAdminClientConfigRuntimeValue.getValue().tlsConfigurationName()).orElse(null);
        final boolean globalTrustAll;
        if (registry.getDefault().isPresent()) {
            globalTrustAll = registry.getDefault().get().isTrustAll();
        } else {
            globalTrustAll = false;
        }

        Keycloak.setClientProvider(new ResteasyClientProvider() {
            @Override
            public Client newRestEasyClient(Object customJacksonProvider, SSLContext sslContext, boolean disableTrustManager) {
                // this is what 'org.keycloak.admin.client.ClientBuilderWrapper.create' does
                var builder = new ResteasyClientBuilderImpl();
                builder.connectionPoolSize(10);

                if (namedTlsConfig == null) {
                    builder.sslContext(sslContext);
                    if (globalTrustAll) {
                        builder.disableTrustManager();
                    }
                } else {
                    if (namedTlsConfig.isTrustAll()) {
                        builder.disableTrustManager();
                    }
                    try {
                        builder.sslContext(namedTlsConfig.createSSLContext());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create Keycloak Admin client SSLContext", e);
                    }
                }

                // point here is to use default Quarkus providers rather than org.keycloak.admin.client.JacksonProvider
                // as it doesn't work properly in native mode
                if (areJSONBProvidersPresent) {
                    // when both Jackson and JSONB providers are present, we need to ensure Jackson is used
                    builder.register(new AppJsonQuarkusJacksonSerializer(), 100);
                }
                return builder.build();
            }

            @Override
            public <R> R targetProxy(WebTarget webTarget, Class<R> aClass) {
                return (ResteasyWebTarget.class.cast(webTarget)).proxy(aClass);
            }
        });
    }

    public void avoidRuntimeInitIssueInClientBuilderWrapper() {
        // we set our provider at runtime, it is not used before that
        // however org.keycloak.admin.client.Keycloak.CLIENT_PROVIDER is initialized during
        // static init with org.keycloak.admin.client.ClientBuilderWrapper that is not compatible with native mode
        Keycloak.setClientProvider(null);
    }

    // makes media type more specific which ensures that it will be used first
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    static class AppJsonQuarkusJacksonSerializer extends QuarkusJacksonSerializer {
    }
}
