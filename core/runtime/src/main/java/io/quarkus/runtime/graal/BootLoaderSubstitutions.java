package io.quarkus.runtime.graal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.hub.ClassForNameSupport;
import com.oracle.svm.core.jdk.JDK11OrLater;

@TargetClass(className = "jdk.internal.loader.BootLoader", onlyWith = JDK11OrLater.class)
final class Target_jdk_internal_loader_BootLoader {

    /*
     * The following substitution is required to work around a NPE that happened when `BootLoader.loadClassOrNull(name)` was
     * called during the Quarkus native integration tests execution. It will be directly fixed in a future GraalVM release
     * which is undetermined for now. This substitution should be removed as soon as Quarkus depends on a GraalVM version that
     * includes the fix.
     */
    @Substitute
    private static Class<?> loadClassOrNull(String name) {
        return ClassForNameSupport.forNameOrNull(name, false);
    }

    /*
     * The following substitution is required to work around a NPE that happened when `BootLoader.hasClassPath()` was called
     * during the Quarkus native integration tests execution. It was fixed in a GraalVM commit which should be backported to
     * 19.3.1 (https://github.com/oracle/graal/commit/68027de). This substitution should be removed as soon as Quarkus depends
     * on a GraalVM version that includes that commit.
     * See https://github.com/oracle/graal/issues/1966 for more details about the NPE.
     */
    @Substitute
    private static boolean hasClassPath() {
        return true;
    }

    @Substitute
    private static URL findResource(String mn, String name) {
        return ClassLoader.getSystemClassLoader().getResource(name);
    }

    @Substitute
    private static InputStream findResourceAsStream(String mn, String name) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(name);
    }

    @Substitute
    private static URL findResource(String name) {
        return ClassLoader.getSystemClassLoader().getResource(name);
    }

    @Substitute
    private static Enumeration<URL> findResources(String name) throws IOException {
        return ClassLoader.getSystemClassLoader().getResources(name);
    }
}
