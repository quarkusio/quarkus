package io.quarkus.vertx.core.runtime.graal;

import java.io.IOException;
import java.net.URL;
import java.util.function.Function;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK11OrLater;
import com.oracle.svm.core.jdk.JDK8OrEarlier;

@TargetClass(className = "URLClassPath$Loader", classNameProvider = Package_jdk_internal_loader.class)
final class Target_URLClassPath$Loader {

    @Alias
    public Target_URLClassPath$Loader(URL url) {
    }
}

@TargetClass(className = "URLClassPath$FileLoader", classNameProvider = Package_jdk_internal_loader.class)
final class Target_URLClassPath$FileLoader {

    @Alias
    public Target_URLClassPath$FileLoader(URL url) throws IOException {
    }
}

@TargetClass(className = "sun.misc.URLClassPath", onlyWith = JDK8OrEarlier.class)
final class Target_sun_misc_URLClassPath {

    @Substitute
    private Target_URLClassPath$Loader getLoader(final URL url) throws IOException {
        String file = url.getFile();
        if (file != null && file.endsWith("/")) {
            if ("file".equals(url.getProtocol())) {
                return (Target_URLClassPath$Loader) (Object) new Target_URLClassPath$FileLoader(
                        url);
            } else {
                return new Target_URLClassPath$Loader(url);
            }
        } else {
            // that must be wrong, but JarLoader is deleted by SVM
            return (Target_URLClassPath$Loader) (Object) new Target_URLClassPath$FileLoader(
                    url);
        }
    }

    @Substitute
    private int[] getLookupCache(String name) {
        return null;
    }
}

@TargetClass(className = "jdk.internal.loader.URLClassPath", onlyWith = JDK11OrLater.class)
final class Target_jdk_internal_loader_URLClassPath {

    @Substitute
    private Target_URLClassPath$Loader getLoader(final URL url) throws IOException {
        String file = url.getFile();
        if (file != null && file.endsWith("/")) {
            if ("file".equals(url.getProtocol())) {
                return (Target_URLClassPath$Loader) (Object) new Target_URLClassPath$FileLoader(
                        url);
            } else {
                return new Target_URLClassPath$Loader(url);
            }
        } else {
            // that must be wrong, but JarLoader is deleted by SVM
            return (Target_URLClassPath$Loader) (Object) new Target_URLClassPath$FileLoader(
                    url);
        }
    }

}

final class Package_jdk_internal_loader implements Function<TargetClass, String> {

    private static final JDK8OrEarlier JDK_8_OR_EARLIER = new JDK8OrEarlier();

    @Override
    public String apply(TargetClass annotation) {
        if (JDK_8_OR_EARLIER.getAsBoolean()) {
            return "sun.misc." + annotation.className();
        }

        return "jdk.internal.loader." + annotation.className();
    }
}

class JdkSubstitutions {

}
