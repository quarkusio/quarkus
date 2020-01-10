package io.quarkus.elytron.security.filesystem.runtime;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * A configuration object for a filesystem based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.FileSystemSecurityRealm}
 */
@ConfigRoot(name = "security.filesystem", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class FilesystemRealmConfig {

    /**
     * If the filesystem store is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The path to the file containing the realm.
     */
    @ConfigItem
    public Optional<Path> path;

    /**
     * The number of levels of directory hashing to apply. Default is 2.
     */
    @ConfigItem(defaultValue = "2")
    public int levels;

    /**
     * Whether the identity names should be stored encoded (Base32) in file names.
     */
    @ConfigItem(defaultValue = "true")
    public boolean encoded;

    /**
     * The realm name.
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;
}
