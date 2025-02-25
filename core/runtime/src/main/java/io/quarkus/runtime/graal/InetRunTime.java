package io.quarkus.runtime.graal;

import java.net.Inet4Address;
import java.net.Inet6Address;

import io.quarkus.runtime.graal.Target_io_smallrye_common_net_CidrAddress.CidrAddressUtil;
import io.smallrye.common.net.Inet;

public class InetRunTime {
    public static final Inet4Address INET4_ANY = Inet.getInet4Address(0, 0, 0, 0);
    public static final Inet4Address INET4_LOOPBACK = Inet.getInet4Address(127, 0, 0, 1);
    public static final Inet4Address INET4_BROADCAST = Inet.getInet4Address(255, 255, 255, 255);
    public static final Inet6Address INET6_ANY = Inet.getInet6Address(0, 0, 0, 0, 0, 0, 0, 0);
    public static final Inet6Address INET6_LOOPBACK = Inet.getInet6Address(0, 0, 0, 0, 0, 0, 0, 1);
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

class Inet4AnyCidrAccessor {

    private static volatile Target_io_smallrye_common_net_CidrAddress INET4_ANY_CIDR;

    static Target_io_smallrye_common_net_CidrAddress get() {
        Target_io_smallrye_common_net_CidrAddress result = INET4_ANY_CIDR;
        if (result == null) {
            // Lazy initialization on first access.
            result = initializeOnce();
        }
        return result;
    }

    private static synchronized Target_io_smallrye_common_net_CidrAddress initializeOnce() {
        Target_io_smallrye_common_net_CidrAddress result = INET4_ANY_CIDR;
        if (result != null) {
            // Double-checked locking is OK because INSTANCE is volatile.
            return result;
        }
        result = CidrAddressUtil.newInstance(Inet.INET4_ANY, 0);
        INET4_ANY_CIDR = result;
        return result;
    }
}

class Inet6AnyCidrAccessor {

    private static volatile Target_io_smallrye_common_net_CidrAddress INET6_ANY_CIDR;

    static Target_io_smallrye_common_net_CidrAddress get() {
        Target_io_smallrye_common_net_CidrAddress result = INET6_ANY_CIDR;
        if (result == null) {
            // Lazy initialization on first access.
            result = initializeOnce();
        }
        return result;
    }

    private static synchronized Target_io_smallrye_common_net_CidrAddress initializeOnce() {
        Target_io_smallrye_common_net_CidrAddress result = INET6_ANY_CIDR;
        if (result != null) {
            // Double-checked locking is OK because INSTANCE is volatile.
            return result;
        }
        result = CidrAddressUtil.newInstance(Inet.INET6_ANY, 0);
        INET6_ANY_CIDR = result;
        return result;
    }
}
