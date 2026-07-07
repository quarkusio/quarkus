package io.quarkus.gradle.application.dsl;

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;

/**
 * Configures the standalone {@code quarkusApplicationRemoteDev} continuous-build session and its internal mutable-JAR
 * package operation.
 * <p>
 * Remote dev requires Gradle continuous build. Remote-dev properties override matching root extension properties for
 * the package and synchronization operations. Fork options extend the root build-worker options used to create the
 * internal mutable JAR; both maps are empty by default.
 */
public abstract class QuarkusApplicationRemoteDev {

    private final QuarkusApplicationDevForkOptions forkOptions;

    /**
     * Creates empty remote-dev property and fork-option conventions.
     *
     * @param objects Gradle's object factory
     */
    @Inject
    public QuarkusApplicationRemoteDev(ObjectFactory objects) {
        this.forkOptions = objects.newInstance(QuarkusApplicationDevForkOptions.class);
        getQuarkusBuildProperties().convention(Map.of());
    }

    /**
     * Returns Quarkus configuration applied to remote-dev operations after root extension properties.
     *
     * @return the lazily configurable remote-dev properties, empty by default
     */
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    /**
     * Returns JVM options appended to the root build-worker options for the internal remote-dev package build.
     *
     * @return the remote-dev package-worker options
     */
    public QuarkusApplicationDevForkOptions getForkOptions() {
        return forkOptions;
    }

    /**
     * Configures JVM options for the internal remote-dev package build.
     *
     * @param action the fork-options configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void forkOptions(Action<? super QuarkusApplicationDevForkOptions> action) {
        action.execute(forkOptions);
    }
}
