package org.jboss.shamrock.vertx.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.vertx.core.net.impl.transport.Transport;

@TargetClass(className = "io.vertx.core.net.impl.transport.Transport")
final class Target_io_vertx_core_net_impl_transport_Transport {
	@Substitute
	public static Transport nativeTransport() {
		return Transport.JDK;
	}
}

class VertxSubstitutions {

}
