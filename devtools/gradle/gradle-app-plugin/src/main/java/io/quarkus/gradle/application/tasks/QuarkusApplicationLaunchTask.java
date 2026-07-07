package io.quarkus.gradle.application.tasks;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.model.QuarkusApplicationLaunchKind;

/**
 * Implementation base identifying the launch mode of a plugin-created run, dev, continuous-test, or remote-dev task.
 * <p>
 * Public visibility is required for Gradle decoration. This abstract type is not a supported typed user entry point and
 * makes no compatibility commitment for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Reserved launch tasks fail immediately and do not produce reusable outputs")
public abstract class QuarkusApplicationLaunchTask extends QuarkusApplicationTask {

    /**
     * Returns the launch behavior fixed by task registration.
     *
     * @return the required launch kind
     */
    @Input
    public abstract Property<QuarkusApplicationLaunchKind> getLaunchKind();
}
