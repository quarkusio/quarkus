package io.quarkus.runtime.graal;

import java.net.Inet4Address;
import java.net.Inet6Address;

import org.wildfly.common.net.Inet;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

/*
 * The following substitutions are required because of a new restriction in GraalVM 19.3.0 that prohibits the presence of
 * java.net.Inet4Address and java.net.Inet6Address in the image heap. Each field annotated with @InjectAccessors is lazily
 * recomputed at runtime on first access while Inet.class can still be initialized during the native image build.
 */
@TargetClass(Inet.class)
final class Target_org_wildfly_common_net_Inet {

    @Alias
    @InjectAccessors(Inet4AnyAccessor.class)
    public static Inet4Address INET4_ANY;

    @Alias
    @InjectAccessors(Inet4LoopbackAccessor.class)
    public static Inet4Address INET4_LOOPBACK;

    @Alias
    @InjectAccessors(Inet4BroadcastAccessor.class)
    public static Inet4Address INET4_BROADCAST;

    @Alias
    @InjectAccessors(Inet6AnyAccessor.class)
    public static Inet6Address INET6_ANY;

    @Alias
    @InjectAccessors(Inet6LoopbackAccessor.class)
    public static Inet6Address INET6_LOOPBACK;
}

final class Inet4AnyAccessor {
    static Inet4Address get() {
        return InetRunTime.INET4_ANY;
    }
}

final class Inet4LoopbackAccessor {
    static Inet4Address get() {
        return InetRunTime.INET4_LOOPBACK;
    }
}

final class Inet4BroadcastAccessor {
    static Inet4Address get() {
        return InetRunTime.INET4_BROADCAST;
    }
}

final class Inet6AnyAccessor {
    static Inet6Address get() {
        return InetRunTime.INET6_ANY;
    }
}

final class Inet6LoopbackAccessor {
    static Inet6Address get() {
        return InetRunTime.INET6_LOOPBACK;
    }
}
