package io.quarkus.runtime.graal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK11OrLater;

/*
 * The `hasClassPath()` method substitution from this class is required to work around a NPE when `BootLoader.hasClassPath()`
 * is called. This was fixed in a GraalVM commit (https://github.com/oracle/graal/commit/68027de) which will be backported to
 * 19.3.1. We copied the entire GraalVM commit content into Quarkus for the sake of consistency.
 * See https://github.com/oracle/graal/issues/1966 for more details about the NPE.
 */
@TargetClass(className = "jdk.internal.loader.BootLoader", onlyWith = JDK11OrLater.class)
final class Target_jdk_internal_loader_BootLoader {

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
