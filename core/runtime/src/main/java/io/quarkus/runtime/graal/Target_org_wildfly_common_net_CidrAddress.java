package io.quarkus.runtime.graal;

import java.net.InetAddress;

import org.wildfly.common.net.CidrAddress;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

/*
 * The following substitutions are required because of a new restriction in GraalVM 19.3.0 that prohibits the presence of
 * java.net.Inet4Address and java.net.Inet6Address in the image heap. Each field annotated with @InjectAccessors is lazily
 * recomputed at runtime on first access while CidrAddress.class can still be initialized during the native image build.
 */
@TargetClass(CidrAddress.class)
final class Target_org_wildfly_common_net_CidrAddress {

    @Alias
    public static Target_org_wildfly_common_net_CidrAddress create(InetAddress networkAddress, int netmaskBits) {
        return null;
    }

    @Alias
    @InjectAccessors(Inet4AnyCidrAccessor.class)
    public static CidrAddress INET4_ANY_CIDR;

    @Alias
    @InjectAccessors(Inet6AnyCidrAccessor.class)
    public static CidrAddress INET6_ANY_CIDR;

    static class CidrAddressUtil {
        static Target_org_wildfly_common_net_CidrAddress newInstance(InetAddress networkAddress, int netmaskBits) {
            return Target_org_wildfly_common_net_CidrAddress.create(networkAddress, netmaskBits);
        }
    }
}
