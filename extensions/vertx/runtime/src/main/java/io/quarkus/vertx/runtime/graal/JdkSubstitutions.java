package io.quarkus.vertx.runtime.graal;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.URL;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "sun.misc.URLClassPath$Loader")
final class Target_sun_misc_URLClassPath$Loader {

    @Alias
    public Target_sun_misc_URLClassPath$Loader(URL url) {
    }
}

@TargetClass(className = "sun.misc.URLClassPath$FileLoader")
final class Target_sun_misc_URLClassPath$FileLoader {

    @Alias
    public Target_sun_misc_URLClassPath$FileLoader(URL url) throws IOException {
    }
}

@TargetClass(className = "sun.misc.URLClassPath")
final class Target_sun_misc_URLClassPath {

    @Substitute
    private Target_sun_misc_URLClassPath$Loader getLoader(final URL url) throws IOException {
        String file = url.getFile();
        if (file != null && file.endsWith("/")) {
            if ("file".equals(url.getProtocol())) {
                return (Target_sun_misc_URLClassPath$Loader) (Object) new Target_sun_misc_URLClassPath$FileLoader(
                        url);
            } else {
                return new Target_sun_misc_URLClassPath$Loader(url);
            }
        } else {
            // that must be wrong, but JarLoader is deleted by SVM
            return (Target_sun_misc_URLClassPath$Loader) (Object) new Target_sun_misc_URLClassPath$FileLoader(
                    url);
        }
    }

    @Substitute
    private int[] getLookupCache(String name) {
        return null;
    }
}

@TargetClass(className = "sun.nio.ch.DatagramChannelImpl", onlyWith = GraalVersion19_0.class)
final class Target_sun_nio_ch_DatagramChannelImpl {

    @Substitute
    private static void disconnect0(FileDescriptor fd, boolean isIPv6)
            throws IOException {
        throw new RuntimeException("Unimplemented: sun.nio.ch.DatagramChannelImpl.disconnect0(FileDescriptor, boolean)");
    }
}

final class GraalVersion19_0 implements BooleanSupplier {
    public boolean getAsBoolean() {
        final String version = System.getProperty("org.graalvm.version");
        return version.startsWith("19.0.");
    }
}

class JdkSubstitutions {

}
