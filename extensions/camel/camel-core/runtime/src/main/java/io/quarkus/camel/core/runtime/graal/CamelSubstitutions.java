package io.quarkus.camel.core.runtime.graal;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

class CamelSubstitutions {
}

@TargetClass(className = "org.apache.camel.util.HostUtils")
final class Target_org_apache_camel_util_HostUtils {

    @Substitute
    private static InetAddress chooseAddress() throws UnknownHostException {
        return InetAddress.getByName("0.0.0.0");
    }
}
