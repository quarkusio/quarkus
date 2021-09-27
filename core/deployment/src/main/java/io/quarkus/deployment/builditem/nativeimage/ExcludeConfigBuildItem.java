package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that allows extension to configure the native-image compiler to effectively
 * ignore certain configuration files in specific jars.
 *
 * The {@code jarFile} property specifies the name of the jar file or a regular expression that can be used to
 * match multiple jar files.
 * Matching jar files using regular expressions should be done as a last resort.
 *
 * The {@code resourceName} property specifies the name of the resource file or a regular expression that can be used to
 * match multiple resource files.
 * For the match to work, the resources need to be part of the matched jar file(s) (see {@code jarFile}).
 * Matching resource files using regular expressions should be done as a last resort.
 *
 * See https://github.com/oracle/graal/pull/3179 for more details.
 */
public final class ExcludeConfigBuildItem extends MultiBuildItem {

    private final String jarFile;
    private final String resourceName;

    public ExcludeConfigBuildItem(String jarFile, String resourceName) {
        this.jarFile = jarFile;
        this.resourceName = resourceName;
    }

    public ExcludeConfigBuildItem(String jarFile) {
        this(jarFile, "/META-INF/native-image/native-image\\.properties");
    }

    public String getJarFile() {
        return jarFile;
    }

    public String getResourceName() {
        return resourceName;
    }
}
