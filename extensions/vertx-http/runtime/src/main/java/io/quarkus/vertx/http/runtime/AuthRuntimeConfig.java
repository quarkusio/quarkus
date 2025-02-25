package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Authentication mechanism information used for configuring HTTP auth instance for the deployment.
 */
public interface AuthRuntimeConfig {
    /**
     * The HTTP permissions
     */
    @WithName("permission")
    Map<String, PolicyMappingConfig> permissions();

    /**
     * The HTTP role based policies
     */
    @WithName("policy")
    Map<String, PolicyConfig> rolePolicy();

    /**
     * Map the `SecurityIdentity` roles to deployment specific roles and add the matching roles to `SecurityIdentity`.
     * <p>
     * For example, if `SecurityIdentity` has a `user` role and the endpoint is secured with a 'UserRole' role,
     * use this property to map the `user` role to the `UserRole` role, and have `SecurityIdentity` to have
     * both `user` and `UserRole` roles.
     */
    @ConfigDocMapKey("role-name")
    Map<String, List<String>> rolesMapping();

    /**
     * Client certificate attribute whose values are going to be mapped to the 'SecurityIdentity' roles
     * according to the roles mapping specified in the certificate properties file.
     * The attribute must be either one of the Relative Distinguished Names (RDNs) or Subject Alternative Names (SANs).
     * By default, the Common Name (CN) attribute value is used for roles mapping.
     * Supported values are:
     * <ul>
     * <li>RDN type - Distinguished Name field. For example 'CN' represents Common Name field.
     * Multivalued RNDs and multiple instances of the same attributes are currently not supported.
     * </li>
     * <li>'SAN_RFC822' - Subject Alternative Name field RFC 822 Name.</li>
     * <li>'SAN_URI' - Subject Alternative Name field Uniform Resource Identifier (URI).</li>
     * <li>'SAN_ANY' - Subject Alternative Name field Other Name.
     * Please note that only simple case of UTF8 identifier mapping is supported.
     * For example, you can map 'other-identifier' to the SecurityIdentity roles.
     * If you use 'openssl' tool, supported Other name definition would look like this:
     * <code>subjectAltName=otherName:1.2.3.4;UTF8:other-identifier</code>
     * </li>
     * </ul>
     */
    @WithDefault("CN")
    String certificateRoleAttribute();

    /**
     * Properties file containing the client certificate attribute value to role mappings.
     * Use it only if the mTLS authentication mechanism is enabled with either
     * `quarkus.http.ssl.client-auth=required` or `quarkus.http.ssl.client-auth=request`.
     * <p/>
     * Properties file is expected to have the `CN_VALUE=role1,role,...,roleN` format and should be encoded using UTF-8.
     */
    Optional<Path> certificateRoleProperties();

    /**
     * The authentication realm
     */
    Optional<String> realm();

    /**
     * Form Auth config
     */
    FormAuthRuntimeConfig form();

    /**
     * Require that all registered HTTP authentication mechanisms must attempt to verify the request credentials.
     * <p>
     * By default, when the {@link #inclusiveMode} is strict, every registered authentication mechanism must produce
     * SecurityIdentity, otherwise, a number of mechanisms which produce the identity may be less than a total
     * number of registered mechanisms.
     * <p>
     * All produced security identities can be retrieved using the following utility method:
     *
     * <pre>
     * {@code
     * io.quarkus.vertx.http.runtime.security.HttpSecurityUtils#getSecurityIdentities(io.quarkus.security.identity.SecurityIdentity)
     * }
     * </pre>
     *
     * An injected `SecurityIdentity` represents an identity produced by the first inclusive authentication mechanism.
     * When the `mTLS` authentication is required, the `mTLS` mechanism is always the first mechanism,
     * because its priority is elevated when inclusive authentication
     * <p>
     * This property is false by default which means that the authentication process is complete as soon as the first
     * `SecurityIdentity` is created.
     * <p>
     * This property will be ignored if the path specific authentication is enabled.
     */
    @WithDefault("false")
    boolean inclusive();

    /**
     * Inclusive authentication mode.
     */
    @WithDefault("strict")
    InclusiveMode inclusiveMode();

    enum InclusiveMode {
        /**
         * Authentication succeeds if at least one of the registered HTTP authentication mechanisms creates the identity.
         */
        LAX,
        /**
         * Authentication succeeds if all the registered HTTP authentication mechanisms create the identity.
         * Typically, inclusive authentication should be in the strict mode when the credentials are carried over mTLS,
         * when both mTLS and another authentication, for example, OIDC bearer token authentication, must succeed.
         * In such cases, `SecurityIdentity` created by the first mechanism, mTLS, can be injected, identities created
         * by other mechanisms will be available on `SecurityIdentity`.
         */
        STRICT
    }
}
