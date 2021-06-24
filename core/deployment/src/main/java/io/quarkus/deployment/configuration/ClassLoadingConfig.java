package io.quarkus.deployment.configuration;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * WARNING: This is not normal quarkus config, this is only read from application.properties.
 * <p>
 * This is because it is needed before any of the config infrastructure is setup.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class ClassLoadingConfig {

    /**
     * Artifacts that are loaded in a parent first manner. This can be used to work around issues where a given
     * class needs to be loaded by the system ClassLoader. Note that if you
     * make a library parent first all its dependencies should generally also be parent first.
     * <p>
     * Artifacts should be configured as a comma separated list of artifact ids, with the group, artifact-id and optional
     * classifier separated by a colon.
     * <p>
     * WARNING: This config property can only be set in application.properties
     */
    @ConfigItem(defaultValue = "")
    public Optional<String> parentFirstArtifacts;

    /**
     * Artifacts that are loaded in the runtime ClassLoader in dev mode, so they will be dropped
     * and recreated on change.
     * <p>
     * This is an advanced option, it should only be used if you have a problem with
     * libraries holding stale state between reloads. Note that if you use this any library that depends on the listed libraries
     * will also need to be reloadable.
     * <p>
     * This setting has no impact on production builds.
     * <p>
     * Artifacts should be configured as a comma separated list of artifact ids, with the group, artifact-id and optional
     * classifier separated by a colon.
     * <p>
     * WARNING: This config property can only be set in application.properties
     */
    @ConfigItem(defaultValue = "")
    public Optional<String> reloadableArtifacts;

    /**
     * Resources that should be removed/hidden from dependencies.
     * <p>
     * This allows for classes and other resources to be removed from dependencies, so they
     * are not accessible to the application. This is a map of artifact id (in the form group:artifact)
     * to a list of resources to be removed.
     * <p>
     * When running in dev and test mode these resources are hidden from the ClassLoader, when running
     * in production mode these files are removed from the jars that contain them.
     * <p>
     * Note that if you want to remove a class you need to specify the class file name. e.g. to
     * remove <code>com.acme.Foo</code> you would specify <code>com/acme/Foo.class</code>.
     * <p>
     * Note that for technical reasons this is not supported when running with JBang.
     */
    @ConfigItem
    public Map<String, Set<String>> removedResources;

}
