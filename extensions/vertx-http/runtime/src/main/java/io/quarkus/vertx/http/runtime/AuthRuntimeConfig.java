package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Authentication mechanism information used for configuring HTTP auth instance for the deployment.
 */
@ConfigGroup
public class AuthRuntimeConfig {

    /**
     * The HTTP permissions
     */
    @ConfigItem(name = "permission")
    public Map<String, PolicyMappingConfig> permissions;

    /**
     * The HTTP role based policies
     */
    @ConfigItem(name = "policy")
    public Map<String, PolicyConfig> rolePolicy;

    /**
     * Map the `SecurityIdentity` roles to deployment specific roles and add the matching roles to `SecurityIdentity`.
     * <p>
     * For example, if `SecurityIdentity` has a `user` role and the endpoint is secured with a 'UserRole' role,
     * use this property to map the `user` role to the `UserRole` role, and have `SecurityIdentity` to have
     * both `user` and `UserRole` roles.
     */
    @ConfigItem
    @ConfigDocMapKey("role-name")
    public Map<String, List<String>> rolesMapping;

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
    @ConfigItem(defaultValue = "CN")
    public String certificateRoleAttribute;

    /**
     * Properties file containing the client certificate attribute value to role mappings.
     * Use it only if the mTLS authentication mechanism is enabled with either
     * `quarkus.http.ssl.client-auth=required` or `quarkus.http.ssl.client-auth=request`.
     * <p/>
     * Properties file is expected to have the `CN_VALUE=role1,role,...,roleN` format and should be encoded using UTF-8.
     */
    @ConfigItem
    public Optional<Path> certificateRoleProperties;

    /**
     * The authentication realm
     */
    @ConfigItem
    public Optional<String> realm;

    /**
     * Form Auth config
     */
    @ConfigItem
    public FormAuthRuntimeConfig form;
}
