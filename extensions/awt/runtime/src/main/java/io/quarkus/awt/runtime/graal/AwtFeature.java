package io.quarkus.awt.runtime.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;

import io.quarkus.runtime.util.JavaVersionUtil;

public class AwtFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        // Added for JDK 19+ due to: https://github.com/openjdk/jdk20/commit/9bc023220 calling FontUtilities
        if (JavaVersionUtil.isJava19OrHigher()) {
            try {
                Class<?> fontUtilitiesClass = Class.forName("sun.font.FontUtilities");
                RuntimeJNIAccess.register(fontUtilitiesClass);
                RuntimeJNIAccess.register(fontUtilitiesClass.getDeclaredFields());
                RuntimeJNIAccess.register(fontUtilitiesClass.getDeclaredMethods());
                RuntimeJNIAccess.register(fontUtilitiesClass.getDeclaredConstructors());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
