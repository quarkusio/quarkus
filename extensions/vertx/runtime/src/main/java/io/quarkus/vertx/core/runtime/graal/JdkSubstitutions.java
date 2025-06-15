package io.quarkus.vertx.core.runtime.graal;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.spi.AsynchronousChannelProvider;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "jdk.internal.loader.URLClassPath$Loader")
final class Target_URLClassPath$Loader {

    @Alias
    public Target_URLClassPath$Loader(URL url) {
    }
}

@TargetClass(className = "jdk.internal.loader.URLClassPath$FileLoader")
final class Target_URLClassPath$FileLoader {

    @Alias
    public Target_URLClassPath$FileLoader(URL url) throws IOException {
    }
}

@TargetClass(className = "jdk.internal.loader.URLClassPath")
final class Target_jdk_internal_loader_URLClassPath {

    @Substitute
    private Target_URLClassPath$Loader getLoader(final URL url) throws IOException {
        String file = url.getFile();
        if (file != null && file.endsWith("/")) {
            if ("file".equals(url.getProtocol())) {
                return (Target_URLClassPath$Loader) (Object) new Target_URLClassPath$FileLoader(url);
            } else {
                return new Target_URLClassPath$Loader(url);
            }
        } else {
            // that must be wrong, but JarLoader is deleted by SVM
            return (Target_URLClassPath$Loader) (Object) new Target_URLClassPath$FileLoader(url);
        }
    }

}

@Substitute
@TargetClass(className = "sun.nio.ch.WindowsAsynchronousFileChannelImpl", innerClass = "DefaultIocpHolder")
@Platforms({ Platform.WINDOWS.class })
final class Target_sun_nio_ch_WindowsAsynchronousFileChannelImpl_DefaultIocpHolder {

    @Alias
    @InjectAccessors(DefaultIocpAccessor.class)
    static Target_sun_nio_ch_Iocp defaultIocp;
}

@TargetClass(className = "sun.nio.ch.Iocp")
@Platforms({ Platform.WINDOWS.class })
final class Target_sun_nio_ch_Iocp {

    @Alias
    Target_sun_nio_ch_Iocp(AsynchronousChannelProvider provider, Target_sun_nio_ch_ThreadPool pool) throws IOException {
    }

    @Alias
    Target_sun_nio_ch_Iocp start() {
        return null;
    }
}

@TargetClass(className = "sun.nio.ch.ThreadPool")
@Platforms({ Platform.WINDOWS.class })
final class Target_sun_nio_ch_ThreadPool {

    @Alias
    static Target_sun_nio_ch_ThreadPool createDefault() {
        return null;
    }
}

final class DefaultIocpAccessor {
    static Target_sun_nio_ch_Iocp get() {
        try {
            return new Target_sun_nio_ch_Iocp(null, Target_sun_nio_ch_ThreadPool.createDefault()).start();
        } catch (IOException ioe) {
            throw new InternalError(ioe);
        }
    }
}

class JdkSubstitutions {

}
