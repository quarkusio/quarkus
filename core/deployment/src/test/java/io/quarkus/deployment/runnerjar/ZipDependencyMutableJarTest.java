package io.quarkus.deployment.runnerjar;

import java.util.Properties;

public class ZipDependencyMutableJarTest extends ZipDependencyFastJarTest {
    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "mutable-jar");
        return props;
    }
}
