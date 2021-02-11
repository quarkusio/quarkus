package io.quarkus.runtime.graal;

import java.net.InetAddress;

import org.wildfly.common.net.CidrAddress;
import org.wildfly.common.net.Inet;

import com.oracle.svm.core.SubstrateUtil;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.graal.Target_org_wildfly_common_net_CidrAddress.CidrAddressUtil;

/*
 * The following substitutions are required because of a new restriction in GraalVM 19.3.0 that prohibits the presence of
 * java.net.Inet4Address and java.net.Inet6Address in the image heap. Each field annotated with @InjectAccessors is lazily
 * recomputed at runtime on first access while CidrAddress.class can still be initialized during the native image build.
 */
@TargetClass(CidrAddress.class)
final class Target_org_wildfly_common_net_CidrAddress {

    @Alias
    private Target_org_wildfly_common_net_CidrAddress(InetAddress networkAddress, int netmaskBits) {
    }

    @Alias
    @InjectAccessors(Inet4AnyCidrAccessor.class)
    public static CidrAddress INET4_ANY_CIDR;

    @Alias
    @InjectAccessors(Inet6AnyCidrAccessor.class)
    public static CidrAddress INET6_ANY_CIDR;

    static class CidrAddressUtil {
        static CidrAddress newInstance(InetAddress networkAddress, int netmaskBits) {
            return SubstrateUtil.cast(new Target_org_wildfly_common_net_CidrAddress(networkAddress, netmaskBits),
                    CidrAddress.class);
        }
    }
}

class Inet4AnyCidrAccessor {

    private static volatile CidrAddress INET4_ANY_CIDR;

    static CidrAddress get() {
        CidrAddress result = INET4_ANY_CIDR;
        if (result == null) {
            // Lazy initialization on first access.
            result = initializeOnce();
        }
        return result;
    }

    private static synchronized CidrAddress initializeOnce() {
        CidrAddress result = INET4_ANY_CIDR;
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

    private static volatile CidrAddress INET6_ANY_CIDR;

    static CidrAddress get() {
        CidrAddress result = INET6_ANY_CIDR;
        if (result == null) {
            // Lazy initialization on first access.
            result = initializeOnce();
        }
        return result;
    }

    private static synchronized CidrAddress initializeOnce() {
        CidrAddress result = INET6_ANY_CIDR;
        if (result != null) {
            // Double-checked locking is OK because INSTANCE is volatile.
            return result;
        }
        result = CidrAddressUtil.newInstance(Inet.INET6_ANY, 0);
        INET6_ANY_CIDR = result;
        return result;
    }
}
