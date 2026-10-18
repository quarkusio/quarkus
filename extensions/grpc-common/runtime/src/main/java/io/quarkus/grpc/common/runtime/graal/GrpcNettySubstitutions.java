package io.quarkus.grpc.common.runtime.graal;

import java.net.URI;
import java.security.Provider;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.NameResolver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.smallrye.common.annotation.SuppressForbidden;

@TargetClass(className = "io.grpc.netty.ProtocolNegotiators")
@SuppressForbidden(reason = "Use original class logging implementation")
final class Target_io_grpc_netty_ProtocolNegotiators {

    @Substitute
    static void logSslEngineDetails(Level level, ChannelHandlerContext ctx, String msg, Throwable t) {
        Logger log = Logger.getLogger("io.grpc.netty.ProtocolNegotiators");
        if (log.isLoggable(level)) {
            log.log(level, msg + "\nNo SSLEngine details available!", t);
        }
    }
}

@TargetClass(className = "io.grpc.netty.GrpcSslContexts")
final class Target_io_grpc_netty_GrpcSslContexts {

    @Substitute
    public static SslContextBuilder configure(SslContextBuilder builder, SslProvider provider) {
        switch (provider) {
            case JDK: {
                Provider jdkProvider = findJdkProvider();
                if (jdkProvider == null) {
                    throw new IllegalArgumentException(
                            "Could not find Jetty NPN/ALPN or Conscrypt as installed JDK providers");
                }
                return configure(builder, jdkProvider);
            }
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }

    @Alias
    private static Provider findJdkProvider() {
        return null;
    }

    @Alias
    public static SslContextBuilder configure(SslContextBuilder builder, Provider jdkProvider) {
        return null;
    }

}

@TargetClass(className = "io.grpc.netty.Utils")
final class Target_io_grpc_netty_Utils {

    @Substitute
    static boolean isEpollAvailable() {
        return false;
    }

    @Substitute
    private static Throwable getEpollUnavailabilityCause() {
        return null;
    }
}

@TargetClass(className = "io.grpc.netty.UdsNameResolverProvider", onlyWith = NoDomainSocketPredicate.class)
final class Target_io_grpc_netty_UdsNameResolverProvider {

    @Substitute
    protected boolean isAvailable() {
        return false;
    }

    @Substitute
    public Object newNameResolver(URI targetUri, NameResolver.Args args) {
        // gRPC calls this method without calling isAvailable, so, make sure we do not touch the UdsNameResolver class.
        // (as it requires domain sockets)
        return null;
    }
}

final class NoDomainSocketPredicate implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            this.getClass().getClassLoader().loadClass("io.netty.channel.unix.DomainSocketAddress");
            return false;
        } catch (Exception ignored) {
            return true;
        }
    }
}

@SuppressWarnings("unused")
class GrpcNettySubstitutions {
}
