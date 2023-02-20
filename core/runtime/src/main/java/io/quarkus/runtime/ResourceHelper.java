package io.quarkus.runtime;

import java.io.InputStream;
import java.lang.reflect.Method;

import org.graalvm.home.Version;

import io.quarkus.runtime.util.ClassPathUtils;

/**
 * Helper method that is invoked from generated bytecode during image processing
 */
public class ResourceHelper {

    public static void registerResources(String resource) {
        resource = resource.replace('\\', '/')); // correct Windows paths
        Version currentGraalVmVersion = Version.getCurrent();
        if (currentGraalVmVersion.compareTo(22, 3) >= 0) {
            // Use the public API RuntimeResourceAccess with GraalVM >= 22.3
            // TODO: Remove reflective access once support for GraalVM < 22.3 gets dropped and directly invoke
            // RuntimeResourceAccess.addResource(ClassLoader.getSystemClassLoader().getUnnamedModule(), resource);
            try {
                Class<?> runtimeResourceSupportClass = Class.forName("org.graalvm.nativeimage.hosted.RuntimeResourceAccess");
                Method addResource = runtimeResourceSupportClass.getDeclaredMethod("addResource", Module.class, String.class);
                addResource.invoke(null, ClassLoader.getSystemClassLoader().getUnnamedModule(), resource);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load resource " + resource, e);
            }
        } else {
            // Use internal API with GraalVM < 22.3
            try {
                Class<?> resourcesClass = Class.forName("com.oracle.svm.core.jdk.Resources");
                Method register = resourcesClass.getDeclaredMethod("registerResource", String.class, InputStream.class);
                ClassPathUtils.consumeAsStreams(ResourceHelper.class.getClassLoader(), resource, in -> {
                    try {
                        register.invoke(null, resource, in);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to register resource " + resource, e);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException("Failed to load resource " + resource, e);
            }
        }
    }

}
