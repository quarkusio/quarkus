package io.quarkus.security.test;

import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm;
import org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm;
import org.wildfly.security.auth.realm.SimpleRealmEntry;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * Basic tests of the properties file based security realm setup
 */
public class SecurityRealmsTestCase {
    @Test
    public void testLegacyPropertiesSecurityRealm() throws IOException {
        InputStream usersStream = SecurityRealmsTestCase.class.getResourceAsStream("/test-users.properties");
        InputStream rolesStream = SecurityRealmsTestCase.class.getResourceAsStream("/test-roles.properties");
        LegacyPropertiesSecurityRealm realm = LegacyPropertiesSecurityRealm.builder()
                .setPlainText(true)
                .setDefaultRealm("default")
                .setUsersStream(usersStream)
                .setGroupsStream(rolesStream)
                .setProviders(new Supplier<Provider[]>() {
                    @Override
                    public Provider[] get() {
                        return new Provider[] { new WildFlyElytronProvider() };
                    }
                })
                .build();
        RealmIdentity scott = realm.getRealmIdentity(new NamePrincipal("scott"));
        PasswordCredential pass = scott.getCredential(PasswordCredential.class);
        ClearPassword clear = ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, "jb0ss".toCharArray());
        Assertions.assertTrue(pass.matches(new PasswordCredential(clear)));
        AuthorizationIdentity auth = scott.getAuthorizationIdentity();
        Attributes roles = auth.getAttributes();
        Assertions.assertTrue(roles.containsValue("groups", "Admin"));
        Assertions.assertTrue(roles.containsValue("groups", "Tester"));

        RealmIdentity jdoe = realm.getRealmIdentity(new NamePrincipal("jdoe"));
        auth = jdoe.getAuthorizationIdentity();
        roles = auth.getAttributes();
        Assertions.assertTrue(roles.containsValue("groups", "NoRolesUser"));
    }

    @Test
    public void testSimpleMapBackedSecurityRealm() throws RealmUnavailableException {
        SimpleMapBackedSecurityRealm realm = new SimpleMapBackedSecurityRealm();
        Map<String, SimpleRealmEntry> identityMap = new HashMap<>();
        Attributes attributes = new MapAttributes();
        attributes.add("groups", 0, "Admin");
        attributes.add("groups", 1, "Tester");
        ClearPassword clear = ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, "jb0ss".toCharArray());
        PasswordCredential jb0ss = new PasswordCredential(clear);
        List<Credential> credentials = new ArrayList<>();
        credentials.add(jb0ss);
        SimpleRealmEntry scottEntry = new SimpleRealmEntry(credentials, attributes);
        identityMap.put("scott", scottEntry);
        realm.setIdentityMap(identityMap);

        RealmIdentity scott = realm.getRealmIdentity(new NamePrincipal("scott"));
        AuthorizationIdentity auth = scott.getAuthorizationIdentity();
        Attributes roles = auth.getAttributes();
        Assertions.assertTrue(roles.containsValue("groups", "Admin"));
        Assertions.assertTrue(roles.containsValue("groups", "Tester"));
    }
}
