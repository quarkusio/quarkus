package io.quarkus.gradle.application.internal.plugin;

import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.GradleException;

import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.nativeimage.NativeResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

final class QuarkusApplicationNativeExecutableResolver {

    private QuarkusApplicationNativeExecutableResolver() {
    }

    static Path resolve(Path resultFile, String suiteName, String expectedBuildName) {
        NativeResult result;
        try {
            result = new NativeResultCodec().read(resultFile);
        } catch (RuntimeException e) {
            throw new GradleException("Quarkus integration-test suite '" + suiteName
                    + "' could not read the native result for build '" + expectedBuildName
                    + "' from " + resultFile, e);
        }
        if (!expectedBuildName.equals(result.buildName())) {
            throw new GradleException("Quarkus integration-test suite '" + suiteName
                    + "' expected native result for build '" + expectedBuildName + "', but " + resultFile
                    + " belongs to build '" + result.buildName() + "'");
        }
        if (result.buildType() != QuarkusApplicationBuildType.NATIVE_EXECUTABLE) {
            throw new GradleException("Quarkus integration-test suite '" + suiteName + "' requires build '"
                    + expectedBuildName + "' to produce a native executable, but its result type is "
                    + result.buildType());
        }
        Path executable = result.executablePath()
                .orElseThrow(() -> new GradleException("Quarkus integration-test suite '" + suiteName
                        + "' cannot run because build '" + expectedBuildName
                        + "' did not produce a native executable"));
        Path absoluteExecutable = executable.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absoluteExecutable)) {
            throw new GradleException("Quarkus integration-test suite '" + suiteName
                    + "' cannot run because build '" + expectedBuildName + "' selected native executable "
                    + absoluteExecutable + ", which is not a regular file");
        }
        return absoluteExecutable;
    }
}
