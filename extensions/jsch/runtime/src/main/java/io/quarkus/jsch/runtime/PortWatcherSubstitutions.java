package io.quarkus.jsch.runtime;

import java.net.InetAddress;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

/*
 * The following substitution is required because of a new restriction in GraalVM 19.3.0 that prohibits the presence of
 * java.net.Inet4Address in the image heap. Each field annotated with @InjectAccessors is lazily recomputed at runtime on first
 * access while PortWatcher.class can still be initialized during the native image build.
 */
@TargetClass(className = "com.jcraft.jsch.PortWatcher")
final class Target_com_jcraft_jsch_PortWatcher {

    @Alias
    @InjectAccessors(AnyLocalAddressAccessor.class)
    private static InetAddress anyLocalAddress;
}

final class AnyLocalAddressAccessor {

    static InetAddress get() {
        return PortWatcherRunTime.anyLocalAddress;
    }
}
