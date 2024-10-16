package io.quarkus.gradle.tasks;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CryptoConfigTest {

    @TempDir
    Path testProjectDir;

    @Test
    @Disabled("To be fixed via https://github.com/quarkusio/quarkus/issues/38007")
    void smallryeCrypto() throws Exception {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/crypto/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("build", "--info", "--stacktrace", "--build-cache", "--configuration-cache")
                // .build() checks whether the build failed, which is good enough for this test
                .build();

    }
}
