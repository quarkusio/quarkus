package io.quarkus.elytron.security.filesystem.runtime;

import java.util.NoSuchElementException;

import org.wildfly.security.auth.realm.FileSystemSecurityRealm;
import org.wildfly.security.auth.server.NameRewriter;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FilesystemRecorder {

    public RuntimeValue<SecurityRealm> createRealm(FilesystemRealmConfig filesystem) {
        FileSystemSecurityRealm realm = new FileSystemSecurityRealm(filesystem.path
                .orElseThrow(() -> new NoSuchElementException("Property quarkus.security.filesystem.path should be specified")),
                NameRewriter.IDENTITY_REWRITER,
                filesystem.levels,
                filesystem.encoded);
        return new RuntimeValue<>(realm);
    }
}
