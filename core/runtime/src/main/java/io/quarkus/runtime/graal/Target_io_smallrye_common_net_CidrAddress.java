package io.quarkus.runtime.graal;

import java.net.InetAddress;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

import io.smallrye.common.net.CidrAddress;

/*
 * The following substitutions are required because of a new restriction in GraalVM 19.3.0 that prohibits the presence of
 * java.net.Inet4Address and java.net.Inet6Address in the image heap. Each field annotated with @InjectAccessors is lazily
 * recomputed at runtime on first access while CidrAddress.class can still be initialized during the native image build.
 */
@TargetClass(CidrAddress.class)
final class Target_io_smallrye_common_net_CidrAddress {

    @Alias
    private Target_io_smallrye_common_net_CidrAddress(InetAddress networkAddress, int netmaskBits) {
    }

    @Alias
    @InjectAccessors(Inet4AnyCidrAccessor.class)
    public static CidrAddress INET4_ANY_CIDR;

    @Alias
    @InjectAccessors(Inet6AnyCidrAccessor.class)
    public static CidrAddress INET6_ANY_CIDR;

    static class CidrAddressUtil {
        static Target_io_smallrye_common_net_CidrAddress newInstance(InetAddress networkAddress, int netmaskBits) {
            return new Target_io_smallrye_common_net_CidrAddress(networkAddress, netmaskBits);
        }
    }
}
