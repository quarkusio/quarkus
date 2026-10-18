package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Extension Dev mode configuration options
 */
public class ExtensionDevModeConfig implements Serializable, Mappable {

    static ExtensionDevModeConfig fromMap(Map<String, Object> map) {
        JvmOptions jvmOptions = null;
        Collection<Map<String, Object>> jvmOptionsMap = (Collection<Map<String, Object>>) map
                .get(BootstrapConstants.MAPPABLE_JVM_OPTIONS);
        if (jvmOptionsMap != null) {
            final JvmOptionsBuilder optionsBuilder = new JvmOptionsBuilder();
            for (Map<String, Object> jvmOptionMap : jvmOptionsMap) {
                optionsBuilder.addAllToGroup(
                        jvmOptionMap.get(BootstrapConstants.MAPPABLE_JVM_OPTION_GROUP_PREFIX).toString(),
                        jvmOptionMap.get(BootstrapConstants.MAPPABLE_NAME).toString(),
                        (Collection<String>) jvmOptionMap.get(BootstrapConstants.MAPPABLE_VALUES));
            }
            jvmOptions = optionsBuilder.build();
        }

        final Collection<String> lockOptions = (Collection<String>) map.get(BootstrapConstants.MAPPABLE_LOCK_JVM_OPTIONS);

        return new ExtensionDevModeConfig(
                ArtifactKey.fromString(map.get(BootstrapConstants.MAPPABLE_EXTENSION).toString()),
                jvmOptions,
                lockOptions == null ? Set.of() : new HashSet<>(lockOptions));
    }

    private final ArtifactKey extensionKey;
    private final JvmOptions jvmOptions;
    private final Set<String> lockJvmOptions;

    public ExtensionDevModeConfig(ArtifactKey extensionKey, JvmOptions jvmOptions, Set<String> lockDefaultJvmOptions) {
        this.extensionKey = extensionKey;
        this.jvmOptions = jvmOptions;
        this.lockJvmOptions = lockDefaultJvmOptions;
    }

    /**
     * Extension key
     *
     * @return extension key
     */
    public ArtifactKey getExtensionKey() {
        return extensionKey;
    }

    /**
     * JVM options that should be added to the command line launching an application in Dev mode.
     *
     * @return JVM options to be added to the command line launching an application in Dev mode
     */
    public JvmOptions getJvmOptions() {
        return jvmOptions;
    }

    /**
     * JVM options whose default values should not be overridden with values that otherwise would be recommended
     * as defaults for Quarkus dev mode by the Quarkus Maven and Gradle plugins.
     *
     * @return JVM options that shouldn't be overridden
     */
    public Set<String> getLockJvmOptions() {
        return lockJvmOptions;
    }

    @Override
    public Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap(3);
        map.put(BootstrapConstants.MAPPABLE_EXTENSION, extensionKey.toGacString());
        if (!jvmOptions.isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_JVM_OPTIONS, Mappable.iterableAsMaps(jvmOptions, factory));
        }
        if (!lockJvmOptions.isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_LOCK_JVM_OPTIONS, Mappable.toStringCollection(lockJvmOptions, factory));
        }
        return map;
    }
}
