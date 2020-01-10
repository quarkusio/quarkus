package io.quarkus.security.test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.realm.FileSystemSecurityRealm;
import org.wildfly.security.auth.server.ModifiableRealmIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.password.spec.ClearPasswordSpec;

public class FilesystemHelper {

    private FileSystemSecurityRealm realm;

    public FilesystemHelper(Path root) {
        this.realm = new FileSystemSecurityRealm(root);
    }

    public void addUser(String name, String pass, String... roles) throws Exception {
        ModifiableRealmIdentity identity = realm.getRealmIdentityForUpdate(new NamePrincipal(name));
        identity.create();
        PasswordFactory factory = PasswordFactory.getInstance(ClearPassword.ALGORITHM_CLEAR);
        Password password = factory.generatePassword(new ClearPasswordSpec(pass.toCharArray()));
        identity.setCredentials(Collections.singleton(new PasswordCredential(password)));
        if (roles.length > 0) {
            MapAttributes newAttributes = new MapAttributes();
            newAttributes.addAll("groups", Arrays.asList(roles));
            identity.setAttributes(newAttributes);
        }
        identity.dispose();
    }
}
