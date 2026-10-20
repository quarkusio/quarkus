package io.quarkus.elytron.security.properties.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.wildfly.common.iteration.ByteIterator;
import org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm;
import org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm;
import org.wildfly.security.auth.realm.SimpleRealmEntry;
import org.wildfly.security.auth.server.NameRewriter;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.password.spec.DigestPasswordSpec;

import io.quarkus.runtime.util.ClassPathUtils;

public class ElytronPropertiesFileRecorder {

    private static final Logger log = Logger.getLogger(ElytronPropertiesFileRecorder.class);

    private static final Provider[] PROVIDERS = new Provider[] { new WildFlyElytronPasswordProvider() };

    /**
     * Load the user.properties and roles.properties files into the {@linkplain SecurityRealm}
     *
     * @param propsRealm - a {@linkplain LegacyPropertiesSecurityRealm}
     * @param propertiesConfig - properties config
     * @throws Exception
     */
    private static void loadRealm(LegacyPropertiesSecurityRealm propsRealm, SecurityUsersConfig propertiesConfig)
            throws Exception {
        try {
            PropertiesRealmConfig config = propertiesConfig.file();
            log.debugf("loadRealm, config=%s", config);
            log.debugf("Trying to loader users: /%s", config.users());
            URL users;
            Path p = Paths.get(config.users());
            if (Files.exists(p)) {
                users = p.toUri().toURL();
            } else {
                users = Thread.currentThread().getContextClassLoader().getResource(config.users());
            }
            log.debugf("users: %s", users);
            log.debugf("Trying to loader roles: %s", config.roles());
            URL roles;
            p = Paths.get(config.roles());
            if (Files.exists(p)) {
                roles = p.toUri().toURL();
            } else {
                roles = Thread.currentThread().getContextClassLoader().getResource(config.roles());
            }
            log.debugf("roles: %s", roles);
            if (users == null && roles == null) {
                String msg = String.format(
                        "No PropertiesRealmConfig users/roles settings found. Configure the quarkus.security.file.%s properties",
                        PropertiesRealmConfig.help());
                throw new IllegalStateException(msg);
            }
            ClassPathUtils.consumeStream(users, usersStream -> {
                try {
                    ClassPathUtils.consumeStream(roles, rolesStream -> {
                        try {
                            propsRealm.load(usersStream, rolesStream);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Load the embedded user and role information into the {@linkplain SecurityRealm}.
     */
    private static void loadEmbeddedRealm(SecurityRealm realm, MPRealmRuntimeConfig runtimeConfig,
            SecurityUsersConfig propertiesConfig) {
        MPRealmConfig config = propertiesConfig.embedded();
        log.debugf("loadRealm, config=%s", config);
        SecurityRealm secRealm = realm;
        if (!(secRealm instanceof SimpleMapBackedSecurityRealm memRealm)) {
            return;
        }
        HashMap<String, SimpleRealmEntry> identityMap = new HashMap<>();
        Map<String, String> userInfo = runtimeConfig.users();
        log.debugf("UserInfoMap: %s%n", userInfo);
        Map<String, String> roleInfo = runtimeConfig.roles();
        log.debugf("RoleInfoMap: %s%n", roleInfo);
        for (Map.Entry<String, String> userPasswordEntry : userInfo.entrySet()) {
            Password password;
            String user = userPasswordEntry.getKey();

            if (runtimeConfig.plainText()) {
                password = ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR,
                        userPasswordEntry.getValue().toCharArray());
            } else {
                try {
                    byte[] hashed = ByteIterator.ofBytes(userPasswordEntry.getValue().getBytes(StandardCharsets.UTF_8))
                            .asUtf8String().hexDecode().drain();

                    password = PasswordFactory
                            .getInstance(runtimeConfig.algorithm().getName(),
                                    new WildFlyElytronPasswordProvider())
                            .generatePassword(new DigestPasswordSpec(user, config.realmName(), hashed));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to register password for user:" + user
                            + " make sure it is a valid hex encoded "
                            + runtimeConfig.algorithm().getName().toUpperCase() + " hash", e);
                }
            }

            PasswordCredential passwordCred = new PasswordCredential(password);
            List<Credential> credentials = new ArrayList<>();
            credentials.add(passwordCred);
            String rawRoles = roleInfo.get(user);
            String[] roles = rawRoles != null ? rawRoles.split(",") : new String[0];
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

    /**
     * Create a {@linkplain LegacyPropertiesSecurityRealm} and load the users and roles properties files into it.
     *
     * @param propertiesConfig - properties config
     * @return - the SecurityRealm
     * @throws Exception
     */
    public static SecurityRealm createRealm(SecurityUsersConfig propertiesConfig) throws Exception {
        PropertiesRealmConfig config = propertiesConfig.file();
        log.debugf("createRealm, config=%s", config);

        LegacyPropertiesSecurityRealm realm = LegacyPropertiesSecurityRealm.builder()
                .setDefaultRealm(config.realmName())
                .setProviders(new Supplier<Provider[]>() {
                    @Override
                    public Provider[] get() {
                        return PROVIDERS;
                    }
                })
                .setPlainText(config.plainText())
                .build();
        loadRealm(realm, propertiesConfig);
        return realm;
    }

    /**
     * Create a security realm value for a {@linkplain SimpleMapBackedSecurityRealm}
     *
     * @return - the SecurityRealm
     * @throws Exception
     */
    public static SecurityRealm createEmbeddedRealm(SecurityUsersConfig propertiesConfig, MPRealmRuntimeConfig runtimeConfig)
            throws Exception {
        MPRealmConfig config = propertiesConfig.embedded();
        log.debugf("createRealm, config=%s", config);

        Supplier<Provider[]> providers = new Supplier<Provider[]>() {
            @Override
            public Provider[] get() {
                return PROVIDERS;
            }
        };
        var realm = new SimpleMapBackedSecurityRealm(NameRewriter.IDENTITY_REWRITER, providers);
        loadEmbeddedRealm(realm, runtimeConfig, propertiesConfig);
        return realm;
    }
}
