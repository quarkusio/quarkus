package io.quarkus.gradle.tasks;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

public class CryptoConfigTest extends BaseGradleTest {

    @Test
    @Disabled("To be fixed via https://github.com/quarkusio/quarkus/issues/38007")
    void smallryeCrypto() throws Exception {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/crypto/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        buildResult(Map.of(), "build", "--info", "--stacktrace", "--build-cache");

    }
}
