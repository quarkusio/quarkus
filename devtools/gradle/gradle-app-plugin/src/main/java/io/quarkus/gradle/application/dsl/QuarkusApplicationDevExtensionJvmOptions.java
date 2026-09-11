package io.quarkus.gradle.application.dsl;

import java.util.List;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * Filters JVM options contributed by Quarkus extensions when launching dev mode.
 * <p>
 * Extension-contributed options are enabled by default. {@link #getDisableAll()} suppresses all of them, while
 * {@link #getDisableFor()} suppresses contributions whose extension coordinates match one of the configured patterns.
 * The owning launch task validates non-empty patterns before starting the child process.
 */
public abstract class QuarkusApplicationDevExtensionJvmOptions {

    /**
     * Creates an enabled-by-default filter with no excluded extension patterns.
     */
    public QuarkusApplicationDevExtensionJvmOptions() {
        getDisableAll().convention(false);
        getDisableFor().convention(List.of());
    }

    /**
     * Returns whether all extension-contributed JVM options are disabled; the convention is {@code false}.
     *
     * @return whether every extension contribution is disabled
     */
    @Input
    public abstract Property<Boolean> getDisableAll();

    /**
     * Returns extension-coordinate patterns whose JVM option contributions are disabled.
     *
     * @return the lazily configurable exclusion patterns, empty by default
     */
    @Input
    public abstract ListProperty<String> getDisableFor();
}
