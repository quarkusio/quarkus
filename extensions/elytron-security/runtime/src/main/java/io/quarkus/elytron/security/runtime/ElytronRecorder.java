package io.quarkus.elytron.security.runtime;

import java.io.IOException;
import java.net.URL;
import java.security.Permission;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm;
import org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm;
import org.wildfly.security.auth.realm.SimpleRealmEntry;
import org.wildfly.security.auth.server.NameRewriter;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.authz.PermissionMappable;
import org.wildfly.security.authz.PermissionMapper;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.permission.PermissionVerifier;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * The runtime security recorder class that provides methods for creating RuntimeValues for the deployment security objects.
 */
@Recorder
public class ElytronRecorder {
    static final Logger log = Logger.getLogger(ElytronRecorder.class);

    /**
     * Load the user.properties and roles.properties files into the {@linkplain SecurityRealm}
     *
     * @param realm - a {@linkplain LegacyPropertiesSecurityRealm}
     * @param config - realm configuration info
     * @throws Exception
     */
    public Runnable loadRealm(RuntimeValue<SecurityRealm> realm, PropertiesRealmConfig config) throws Exception {
        return new Runnable() {
            @Override
            public void run() {
                log.debugf("loadRealm, config=%s", config);
                SecurityRealm secRealm = realm.getValue();
                if (!(secRealm instanceof LegacyPropertiesSecurityRealm)) {
                    return;
                }
                log.debugf("Trying to loader users: /%s", config.users);
                URL users = Thread.currentThread().getContextClassLoader().getResource(config.users);
                log.debugf("users: %s", users);
                log.debugf("Trying to loader roles: %s", config.roles);
                URL roles = Thread.currentThread().getContextClassLoader().getResource(config.roles);
                log.debugf("roles: %s", roles);
                if (users == null && roles == null) {
                    String msg = String.format(
                            "No PropertiesRealmConfig users/roles settings found. Configure the quarkus.security.file.%s properties",
                            config.help());
                    throw new IllegalStateException(msg);
                }
                LegacyPropertiesSecurityRealm propsRealm = (LegacyPropertiesSecurityRealm) secRealm;
                try {
                    propsRealm.load(users.openStream(), roles.openStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Load the embedded user and role information into the {@linkplain SecurityRealm}
     *
     * @param realm - a {@linkplain SimpleMapBackedSecurityRealm}
     * @param config - the realm config
     * @throws Exception
     */
    public Runnable loadRealm(RuntimeValue<SecurityRealm> realm, MPRealmConfig config) throws Exception {
        return new Runnable() {
            @Override
            public void run() {
                log.debugf("loadRealm, config=%s", config);
                SecurityRealm secRealm = realm.getValue();
                if (!(secRealm instanceof SimpleMapBackedSecurityRealm)) {
                    return;
                }
                SimpleMapBackedSecurityRealm memRealm = (SimpleMapBackedSecurityRealm) secRealm;
                HashMap<String, SimpleRealmEntry> identityMap = new HashMap<>();
                Map<String, String> userInfo = config.getUsers();
                log.debugf("UserInfoMap: %s%n", userInfo);
                Map<String, String> roleInfo = config.getRoles();
                log.debugf("RoleInfoMap: %s%n", roleInfo);
                for (Map.Entry<String, String> userPasswordEntry : userInfo.entrySet()) {
                    String user = userPasswordEntry.getKey();
                    ClearPassword clear = ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR,
                            userPasswordEntry.getValue().toCharArray());
                    PasswordCredential passwordCred = new PasswordCredential(clear);
                    List<Credential> credentials = new ArrayList<>();
                    credentials.add(passwordCred);
                    String rawRoles = roleInfo.get(user);
                    String[] roles = rawRoles.split(",");
                    Attributes attributes = new MapAttributes();
                    for (String role : roles) {
                        attributes.addLast("groups", role);
                    }
                    SimpleRealmEntry entry = new SimpleRealmEntry(credentials, attributes);
                    identityMap.put(user, entry);
                    log.debugf("Added user(%s), roles=%s%n", user, attributes.get("groups"));
                }
                memRealm.setIdentityMap(identityMap);
            }
        };
    }

    public void runLoadTask(Runnable runnable) {
        runnable.run();
    }

    /**
     * Create a runtime value for a {@linkplain LegacyPropertiesSecurityRealm}
     *
     * @param config - the realm config
     * @return - runtime value wrapper for the SecurityRealm
     * @throws Exception
     */
    public RuntimeValue<SecurityRealm> createRealm(PropertiesRealmConfig config) throws Exception {
        log.debugf("createRealm, config=%s", config);

        SecurityRealm realm = LegacyPropertiesSecurityRealm.builder()
                .setDefaultRealm("default")
                .setProviders(new Supplier<Provider[]>() {
                    @Override
                    public Provider[] get() {
                        return new Provider[] { new WildFlyElytronProvider() };
                    }
                })
                .setPlainText(true)
                .build();
        return new RuntimeValue<>(realm);
    }

    public void setDomainForIdentityProvider(BeanContainer bc, RuntimeValue<SecurityDomain> domain) {
        bc.instance(ElytronSecurityDomainManager.class).setDomain(domain.getValue());
    }

    /**
     * Create a runtime value for a {@linkplain SimpleMapBackedSecurityRealm}
     *
     * @param config - the realm config
     * @return - runtime value wrapper for the SecurityRealm
     * @throws Exception
     */
    public RuntimeValue<SecurityRealm> createRealm(MPRealmConfig config) {
        log.debugf("createRealm, config=%s", config);

        Supplier<Provider[]> providers = new Supplier<Provider[]>() {
            @Override
            public Provider[] get() {
                return new Provider[] { new WildFlyElytronProvider() };
            }
        };
        SecurityRealm realm = new SimpleMapBackedSecurityRealm(NameRewriter.IDENTITY_REWRITER, providers);
        return new RuntimeValue<>(realm);
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
                .setRoleDecoder(new DefaultRoleDecoder())
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
}
