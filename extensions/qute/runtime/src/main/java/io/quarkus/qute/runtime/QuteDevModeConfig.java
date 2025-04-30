package io.quarkus.qute.runtime;

import java.util.Optional;
import java.util.regex.Pattern;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface QuteDevModeConfig {

    /**
     * By default, a template modification results in an application restart that triggers build-time validations.
     * <p>
     * This regular expression can be used to specify the templates for which the application is not restarted.
     * I.e. the templates are reloaded and only runtime validations are performed.
     * <p>
     * The matched input is the template path that starts with a template root, and the {@code /} is used as a path separator.
     * For example, {@code templates/foo.html}.
     */
    Optional<Pattern> noRestartTemplates();

}