package io.quarkus.deployment.dev;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility methods for working with annotation processor providers.
 */
public class AnnotationProcessorPaths {

    /**
     * Extracts the Maven coordinate (groupId:artifactId) of the extension
     * that provides the given AnnotationProcessorProvider.
     *
     * @param provider the provider instance
     * @return the Maven coordinate, or the provider's class name if not found
     */
    public static String getExtensionCoordinate(AnnotationProcessorProvider provider) {
        try {
            // Find pom.properties in the same JAR as the provider class
            Class<?> providerClass = provider.getClass();
            String className = providerClass.getName();
            String classResource = "/" + className.replace('.', '/') + ".class";
            URL classUrl = providerClass.getResource(classResource);

            if (classUrl != null && "jar".equals(classUrl.getProtocol())) {
                String jarPath = classUrl.getPath().substring(0, classUrl.getPath().indexOf("!"));
                try (JarFile jar = new JarFile(new File(new URL(jarPath).toURI()))) {
                    // Find pom.properties in META-INF/maven/**
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith("META-INF/maven/") &&
                                entry.getName().endsWith("/pom.properties")) {
                            Properties props = new Properties();
                            props.load(jar.getInputStream(entry));
                            String groupId = props.getProperty("groupId");
                            String artifactId = props.getProperty("artifactId");
                            if (groupId != null && artifactId != null) {
                                return groupId + ":" + artifactId;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore - caller can log if needed
        }
        return provider.getClass().getName(); // Fallback to class name
    }

    private AnnotationProcessorPaths() {
        // Utility class
    }
}
