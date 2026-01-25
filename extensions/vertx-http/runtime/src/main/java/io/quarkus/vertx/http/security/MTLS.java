package io.quarkus.vertx.http.security;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.CertificateRoleAttribute;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.quarkus.vertx.http.runtime.security.MtlsAuthenticationMechanism;
import io.smallrye.common.annotation.Experimental;
import io.vertx.core.http.ClientAuth;

/**
 * This class provides a way to create the mutual TLS client authentication mechanism. The {@link HttpAuthenticationMechanism}
 * created with this class can be registered using the {@link HttpSecurity#mechanism(HttpAuthenticationMechanism)} method.
 */
@Experimental("This API is currently experimental and might get changed")
public interface MTLS {

    /**
     * Creates the mutual TLS client authentication mechanism.
     * The client authentication is accepted if presented by a client.
     * Use this method if the client authentication is only required for certain routes
     * and secure these routes with HTTP permissions or standard security annotations.
     *
     * @param tlsConfigurationName the name of the configuration, cannot be {@code null}, cannot be {@code <default>}
     * @return MtlsAuthenticationMechanism
     * @see VertxHttpBuildTimeConfig#tlsClientAuth() for more information about {@link ClientAuth#REQUEST}
     * @see Builder#tls(String) about the TLS configuration name parameter
     */
    static MtlsAuthenticationMechanism request(String tlsConfigurationName) {
        return builder().authentication(ClientAuth.REQUEST).tls(tlsConfigurationName).build();
    }

    /**
     * Creates the mutual TLS client authentication mechanism.
     * The client authentication is accepted if presented by a client.
     * Use this method if the client authentication is only required for certain routes
     * and secure these routes with HTTP permissions or standard security annotations.
     *
     * @param tlsConfigurationName the name of the configuration, cannot be {@code null}, cannot be {@code <default>}
     * @param tlsConfiguration the configuration cannot be {@code null}
     * @return MtlsAuthenticationMechanism
     * @see VertxHttpBuildTimeConfig#tlsClientAuth() for more information about {@link ClientAuth#REQUEST}
     * @see Builder#tls(String, TlsConfiguration) for information about method parameters and TLS config registration
     */
    static MtlsAuthenticationMechanism request(String tlsConfigurationName, TlsConfiguration tlsConfiguration) {
        return builder().authentication(ClientAuth.REQUEST).tls(tlsConfigurationName, tlsConfiguration).build();
    }

    /**
     * Creates the mutual TLS client authentication mechanism.
     * The client authentication is accepted if presented by a client.
     * Use this method if the client authentication is only required for certain routes
     * and secure these routes with HTTP permissions or standard security annotations.
     *
     * @return MtlsAuthenticationMechanism
     * @see VertxHttpBuildTimeConfig#tlsClientAuth() for more information
     */
    static MtlsAuthenticationMechanism request() {
        return builder().authentication(ClientAuth.REQUEST).build();
    }

    /**
     * Creates the mutual TLS client authentication mechanism. This mechanism always require the client authentication.
     *
     * @param tlsConfigurationName the name of the configuration, cannot be {@code null}, cannot be {@code <default>}
     * @param tlsConfiguration the configuration cannot be {@code null}
     * @return MtlsAuthenticationMechanism
     * @see VertxHttpBuildTimeConfig#tlsClientAuth() for more information about {@link ClientAuth#REQUIRED}
     * @see Builder#tls(String, TlsConfiguration) for information about method parameters and TLS config registration
     */
    static MtlsAuthenticationMechanism required(String tlsConfigurationName, TlsConfiguration tlsConfiguration) {
        return builder().tls(tlsConfigurationName, tlsConfiguration).build();
    }

    /**
     * Creates the mutual TLS client authentication mechanism. This mechanism always require the client authentication.
     *
     * @param tlsConfigurationName the name of the configuration, cannot be {@code null}, cannot be {@code <default>}
     * @return MtlsAuthenticationMechanism
     * @see VertxHttpBuildTimeConfig#tlsClientAuth() for more information about {@link ClientAuth#REQUIRED}
     * @see Builder#tls(String) about the TLS configuration name parameter
     */
    static MtlsAuthenticationMechanism required(String tlsConfigurationName) {
        return builder().tls(tlsConfigurationName).build();
    }

    /**
     * Creates the mutual TLS client authentication mechanism.
     * This mechanism always require the client authentication.
     *
     * @return MtlsAuthenticationMechanism
     * @see VertxHttpBuildTimeConfig#tlsClientAuth() for more information
     */
    static MtlsAuthenticationMechanism required() {
        return builder().build();
    }

    /**
     * @return Builder for the mutual TLS client authentication mechanism.
     *         By default, this builder creates a mechanism which requires client authentication.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * The mutual TLS client authentication mechanism builder.
     */
    final class Builder {

        private ClientAuth clientAuth;
        private Function<X509Certificate, Set<String>> certificateToRolesMapper;
        private String certificateAttribute;
        private Map<String, Set<String>> certificateAttributeValueToRoles;
        private Optional<String> httpServerTlsConfigName;
        private TlsConfiguration tlsConfiguration;
        private Optional<Integer> priority;

        public Builder() {
            this.certificateAttribute = null;
            this.certificateToRolesMapper = null;
            this.clientAuth = ClientAuth.REQUIRED;
            this.certificateAttributeValueToRoles = null;
            this.httpServerTlsConfigName = Optional.empty();
            this.tlsConfiguration = null;
            this.priority = Optional.empty();
        }

        /**
         * Configures the name of the TLS configuration used by the HTTP server for the TLS communication.
         * Please note that this method is mutually exclusive with the 'quarkus.http.tls-configuration-name'
         * configuration property.
         *
         * @param tlsConfigurationName the name of the configuration, cannot be {@code <default>}
         * @return Builder
         * @see VertxHttpConfig#tlsConfigurationName() for more information
         */
        public Builder tls(String tlsConfigurationName) {
            if (tlsConfiguration != null) {
                throw new IllegalStateException("TLS configuration is already set");
            }
            this.httpServerTlsConfigName = Optional.ofNullable(tlsConfigurationName);
            return this;
        }

        /**
         * Registers a TLS configuration into the registry and configures the TLS configuration used by the HTTP server
         * for the TLS communication. Please note that this method is mutually exclusive with
         * the 'quarkus.http.tls-configuration-name' configuration property and if the configuration with this name
         * is already registered in the TLS registry, validation will fail.
         * <p>
         * The passed TLS configuration is not validated, so it's up to the caller to ensure the configuration is correct.
         *
         * @param tlsConfigurationName the name of the configuration, cannot be {@code null}, cannot be {@code <default>}
         * @param tlsConfiguration the configuration cannot be {@code null}
         * @return Builder
         * @see io.quarkus.tls.TlsConfigurationRegistry#register(String, TlsConfiguration)
         * @see VertxHttpConfig#tlsConfigurationName()
         */
        public Builder tls(String tlsConfigurationName, TlsConfiguration tlsConfiguration) {
            Objects.requireNonNull(tlsConfiguration);
            Objects.requireNonNull(tlsConfigurationName);
            if (httpServerTlsConfigName.isPresent()) {
                throw new IllegalArgumentException("TLS configuration name has already been configured with the 'tls' method");
            }
            this.httpServerTlsConfigName = Optional.of(tlsConfigurationName);
            this.tlsConfiguration = tlsConfiguration;
            return this;
        }

        /**
         * When the mutual TLS client authentication is configured with this builder, the client authentication
         * is {@link ClientAuth#REQUIRED} for all requests by default. If you configure {@link ClientAuth#REQUEST},
         * the client authentication is accepted if presented by a client.
         * Use the {@link ClientAuth#REQUEST} option if the client authentication is only required for certain routes
         * and secure these routes with HTTP permissions or standard security annotations.
         *
         * @param clientAuthentication {@link ClientAuth#REQUEST} or {@link ClientAuth#REQUIRED}
         * @return Builder
         * @see VertxHttpBuildTimeConfig#tlsClientAuth() for more information
         */
        public Builder authentication(ClientAuth clientAuthentication) {
            Objects.requireNonNull(clientAuthentication);
            if (clientAuthentication == ClientAuth.NONE) {
                throw new IllegalArgumentException("Client authentication cannot be disabled with this API");
            }
            this.clientAuth = clientAuthentication;
            return this;
        }

        /**
         * Selects a certificate attribute which values are mapped to the {@link SecurityIdentity} roles.
         * This attribute will be used for mappings added with the {@link #rolesMapping(String, Set)} method.
         * The default attribute value is configured to the default
         * value of the {@link AuthRuntimeConfig#certificateRoleAttribute()} configuration property.
         *
         * @param certificateAttribute certificate attribute; see {@link AuthRuntimeConfig#certificateRoleAttribute()}
         *        for information about supported values
         * @return CertificateRolesBuilder
         */
        public Builder certificateAttribute(String certificateAttribute) {
            assertCertificateToRolesMapperNotSetYet();
            this.certificateAttribute = Objects.requireNonNull(certificateAttribute);
            return this;
        }

        /**
         * This is a shortcut method for {@code rolesMapping(String, Set.of(roles))}.
         *
         * @return Builder
         * @see #rolesMapping(String, Set) for more information
         */
        public Builder rolesMapping(String certificateAttributeValue, String... roles) {
            return rolesMapping(certificateAttributeValue, Set.of(roles));
        }

        /**
         * Adds a certificate attribute value to roles mapping.
         * The certificate attribute itself can be configured with the {@link #certificateAttribute} method.
         *
         * @param certificateAttributeValue {@link AuthRuntimeConfig#certificateRoleAttribute()} values that will be
         *        mapped to the {@link SecurityIdentity} roles
         * @param roles {@link SecurityIdentity#getRoles()}
         * @return CertificateRolesBuilder
         */
        public Builder rolesMapping(String certificateAttributeValue, Set<String> roles) {
            Objects.requireNonNull(certificateAttributeValue);
            if (roles == null || roles.isEmpty()) {
                throw new IllegalArgumentException("Roles cannot be null or empty");
            }
            assertCertificateToRolesMapperNotSetYet();
            if (certificateAttributeValueToRoles == null) {
                certificateAttributeValueToRoles = new HashMap<>();
            }
            certificateAttributeValueToRoles.computeIfAbsent(certificateAttributeValue, new Function<String, Set<String>>() {
                @Override
                public Set<String> apply(String ignored) {
                    return new HashSet<>();
                }
            }).addAll(roles);
            return this;
        }

        private void assertCertificateToRolesMapperNotSetYet() {
            if (certificateToRolesMapper != null) {
                throw new IllegalStateException(
                        "The certificate to roles mapper is already configured with the 'certificateToRolesMapper' method");
            }
        }

        /**
         * Check the values of different client certificate attributes and map them to the {@link SecurityIdentity} roles.
         *
         * @param certificateToRolesMapper a client certificate to the {@link SecurityIdentity} roles mapper
         * @return Builder
         */
        public Builder certificateToRolesMapper(Function<X509Certificate, Set<String>> certificateToRolesMapper) {
            if (certificateAttributeValueToRoles != null) {
                throw new IllegalStateException(
                        "The certificate to roles mapper is already configured with the 'rolesMapping' method");
            }
            assertCertificateToRolesMapperNotSetYet();
            this.certificateToRolesMapper = certificateToRolesMapper;
            return this;
        }

        /**
         * Mutual TLS authentication mechanism priority.
         *
         * @param priority {@link MtlsAuthenticationMechanism#getPriority()}
         * @return Builder
         * @see AuthRuntimeConfig#mTlsPriority()
         */
        public Builder priority(int priority) {
            this.priority = Optional.of(priority);
            return this;
        }

        /**
         * @return MtlsAuthenticationMechanism that can be registered
         *         with the {@link HttpSecurity#mTLS(MtlsAuthenticationMechanism)} method.
         */
        public MtlsAuthenticationMechanism build() {
            if (certificateAttributeValueToRoles != null) {
                if (certificateAttribute == null) {
                    certificateAttribute = getDefaultCertificateAttributeValue();
                }
                certificateToRolesMapper = new CertificateRoleAttribute(certificateAttribute, certificateAttributeValueToRoles)
                        .rolesMapper();
            }
            var mTlsConfig = new MTLSConfig(certificateToRolesMapper, clientAuth, httpServerTlsConfigName, tlsConfiguration,
                    priority);
            return new MtlsAuthenticationMechanism(mTlsConfig);
        }

        // purpose of this class is to assure that users only build the mTLS mechanism with our public API
        public static final class MTLSConfig {
            public final Function<X509Certificate, Set<String>> certificateToRoles;
            public final ClientAuth tlsClientAuth;
            public final Optional<String> httpServerTlsConfigName;
            public final TlsConfiguration initialTlsConfiguration;
            public final Optional<Integer> priority;

            // please keep this constructor private, so that we have flexibility in classes that are not part of public API
            private MTLSConfig(Function<X509Certificate, Set<String>> certificateToRoles, ClientAuth tlsClientAuth,
                    Optional<String> httpServerTlsConfigName, TlsConfiguration initialTlsConfiguration,
                    Optional<Integer> priority) {
                this.certificateToRoles = certificateToRoles;
                this.tlsClientAuth = tlsClientAuth;
                this.httpServerTlsConfigName = httpServerTlsConfigName;
                this.initialTlsConfiguration = initialTlsConfiguration;
                this.priority = priority;
            }
        }

        /**
         * @return default value as hardcoded in the {@link AuthRuntimeConfig} mapping.
         */
        private static String getDefaultCertificateAttributeValue() {
            return HttpSecurityUtils.getDefaultAuthConfig().auth().certificateRoleAttribute();
        }
    }

}
