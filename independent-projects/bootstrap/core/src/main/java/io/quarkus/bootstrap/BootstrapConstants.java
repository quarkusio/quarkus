package io.quarkus.bootstrap;

/**
 *
 * @author Alexey Loubyansky
 */
public interface BootstrapConstants {

    String DESCRIPTOR_FILE_NAME = "quarkus-extension.properties";

    String META_INF = "META-INF";

    String DESCRIPTOR_PATH = META_INF + '/' + DESCRIPTOR_FILE_NAME;

    String PROP_DEPLOYMENT_ARTIFACT = "deployment-artifact";

    String EMPTY = "";
    String JAR = "jar";
    String POM = "pom";
}
