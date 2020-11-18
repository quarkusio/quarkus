package io.quarkus.deployment.pkg.steps;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;

final class StepUtil {

    private StepUtil() {
    }

    // copied from Java 9
    // TODO remove when we move to Java 11
    static final File NULL_FILE = new File(SystemUtils.IS_OS_WINDOWS ? "NUL" : "/dev/null");
}
