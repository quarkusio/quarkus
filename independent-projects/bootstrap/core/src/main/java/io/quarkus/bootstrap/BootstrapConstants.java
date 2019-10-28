package io.quarkus.bootstrap;

/**
 *
 * @author Alexey Loubyansky
 */
public interface BootstrapConstants {

    @Deprecated
    String DESCRIPTOR_FILE_NAME = "quarkus-extension.properties";
    
    @Deprecated
    String EXTENSION_PROPS_JSON_FILE_NAME = "quarkus-extension.json";
    
    String QUARKUS_EXTENSION_FILE_NAME = "quarkus-extension.yaml";
    

    String META_INF = "META-INF";

    @Deprecated
    String DESCRIPTOR_PATH = META_INF + '/' + DESCRIPTOR_FILE_NAME;

    String PROP_DEPLOYMENT_ARTIFACT = "deployment-artifact";

    String EMPTY = "";
    String JAR = "jar";
    String POM = "pom";
}
