package io.quarkus.elytron.security.runtime;

import java.security.Permission;
import java.security.Security;

import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.PermissionMappable;
import org.wildfly.security.authz.PermissionMapper;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.permission.PermissionVerifier;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * The runtime security recorder class that provides methods for creating RuntimeValues for the deployment security objects.
 */
@Recorder
public class ElytronRecorder {
    static final Logger log = Logger.getLogger(ElytronRecorder.class);

    public void runLoadTask(Runnable runnable) {
        runnable.run();
    }

    public void setDomainForIdentityProvider(BeanContainer bc, RuntimeValue<SecurityDomain> domain) {
        bc.instance(ElytronSecurityDomainManager.class).setDomain(domain.getValue());
    }

    /**
     * Create a {@linkplain SecurityDomain.Builder} for the given default {@linkplain SecurityRealm}.
     *
     * @param realmName - the default realm name
     * @param realm - the default SecurityRealm
     * @return a runtime value for the SecurityDomain.Builder
     * @throws Exception on any error
     */
    public RuntimeValue<SecurityDomain.Builder> configureDomainBuilder(String realmName, RuntimeValue<SecurityRealm> realm)
            throws Exception {
        log.debugf("buildDomain, realm=%s", realm.getValue());

        SecurityDomain.Builder domain = SecurityDomain.builder()

                .addRealm(realmName, realm.getValue())

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

        return new RuntimeValue<>(domain);
    }

    /**
     * Called to add an additional realm to the {@linkplain SecurityDomain} being built
     *
     * @param builder - runtime value for SecurityDomain.Builder created by
     *        {@linkplain #configureDomainBuilder(String, RuntimeValue)}
     * @param realmName - the name of the SecurityRealm
     * @param realm - the runtime value for the SecurityRealm
     */
    public void addRealm(RuntimeValue<SecurityDomain.Builder> builder, String realmName, RuntimeValue<SecurityRealm> realm) {
        builder.getValue().addRealm(realmName, realm.getValue());
    }

    /**
     * Called to invoke the builder created by {@linkplain #configureDomainBuilder(String, RuntimeValue)}
     *
     * @param builder - the security domain builder
     * @return the security domain runtime value
     */
    public RuntimeValue<SecurityDomain> buildDomain(RuntimeValue<SecurityDomain.Builder> builder) {
        return new RuntimeValue<>(builder.getValue().build());
    }

    /**
     * As of Graal 19.3.0 this has to be registered at runtime, due to a bug.
     *
     * 19.3.1 should fix this, see https://github.com/oracle/graal/issues/1883
     */
    public void registerPasswordProvider(ShutdownContext context) {
        WildFlyElytronPasswordProvider provider = new WildFlyElytronPasswordProvider();
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                Security.removeProvider(provider.getName());
            }
        });
        Security.addProvider(provider);
    }
}
