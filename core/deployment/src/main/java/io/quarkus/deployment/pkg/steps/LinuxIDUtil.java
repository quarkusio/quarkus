package io.quarkus.deployment.pkg.steps;

import io.smallrye.common.process.ProcessBuilder;

final class LinuxIDUtil {

    private LinuxIDUtil() {
    }

    static String getLinuxID(String option) {
        try {
            return ProcessBuilder.execToString("id", option).trim();
        } catch (Exception e) {
            //swallow and return null id
            return null;
        }
    }
}
