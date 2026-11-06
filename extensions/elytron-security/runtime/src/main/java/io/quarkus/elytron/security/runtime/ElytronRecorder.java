package io.quarkus.elytron.security.runtime;

import java.security.Permission;
import java.util.Set;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.RealmMapper;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.PermissionMappable;
import org.wildfly.security.authz.PermissionMapper;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.permission.PermissionVerifier;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Runtime helpers used by the Elytron security services to assemble the {@linkplain SecurityDomain} and to hand it
 * over to the identity providers.
 */
public class ElytronRecorder {
    static final Logger log = Logger.getLogger(ElytronRecorder.class);

    private static final String DEFAULT_REALM_NAME_PROPERTY = "quarkus.security.elytron.default-realm-name";

    public static void setDomainForIdentityProvider(BeanContainer bc, SecurityDomain domain) {
        bc.beanInstance(ElytronSecurityDomainManager.class).setDomain(domain);
    }

    public static String getDefaultRealmName(Set<String> realmNames, ElytronSecurityRuntimeConfig config) {
        if (realmNames.size() == 1) {
            // there is no other realm to choose from
            return realmNames.iterator().next();
        }
        String defaultRealmName = config.defaultRealmName();
        if (!realmNames.contains(defaultRealmName)) {
            throw new ConfigurationException("Multiple Elytron security realms are enabled (" + String.join(", ", realmNames)
                    + "), but none of them is named '" + defaultRealmName + "', which is the name of the default realm."
                    + " Set the '" + DEFAULT_REALM_NAME_PROPERTY + "' configuration property to the name of the realm"
                    + " that should be the default realm.", Set.of(DEFAULT_REALM_NAME_PROPERTY));
        }
        return defaultRealmName;
    }

    /**
     * Create a {@linkplain SecurityDomain.Builder} for the given default {@linkplain SecurityRealm}.
     *
     * @param realmName - the default realm name
     * @param realm - the default SecurityRealm
     * @return the SecurityDomain.Builder
     * @throws Exception on any error
     */
    public static SecurityDomain.Builder configureDomainBuilder(String realmName, SecurityRealm realm)
            throws Exception {
        log.debugf("buildDomain, realm=%s", realm);

        SecurityDomain.Builder domain = SecurityDomain.builder()

                .addRealm(realmName, realm)

                .setRoleDecoder(new RoleDecoder() {
                    @Override
                    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
                        return CDI.current().select(DefaultRoleDecoder.class).get().decodeRoles(authorizationIdentity);
                    }
                })
                .build()
                .setDefaultRealmName(realmName)
                .setPermissionMapper(new PermissionMapper() {
                    @Override
                    public PermissionVerifier mapPermissions(PermissionMappable permissionMappable, Roles roles) {
                        return new PermissionVerifier() {
                            @Override
                            public boolean implies(Permission permission) {
                                return true;
                            }
                        };
                    }
                });
        if (CDI.current().select(RealmMapper.class).isResolvable()) {
            domain.setRealmMapper(CDI.current().select(RealmMapper.class).get());
        }

        return domain;
    }

    /**
     * Called to add a realm to the {@linkplain SecurityDomain} being built
     *
     * @param builder - SecurityDomain.Builder
     * @param realmName - the name of the SecurityRealm
     * @param realm - the SecurityRealm
     */
    public static void addRealm(SecurityDomain.Builder builder, String realmName, SecurityRealm realm) {
        builder.addRealm(realmName, realm).setRoleDecoder(new RoleDecoder() {
            @Override
            public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
                return CDI.current().select(DefaultRoleDecoder.class).get().decodeRoles(authorizationIdentity);
            }
        }).build();
    }

}
