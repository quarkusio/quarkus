package io.quarkus.deployment.pkg.jar;

import java.io.IOException;

import io.quarkus.builder.item.BuildItem;

public interface JarBuilder<T extends BuildItem> {

    String DOT_JAR = ".jar";

    T build() throws IOException;
}
