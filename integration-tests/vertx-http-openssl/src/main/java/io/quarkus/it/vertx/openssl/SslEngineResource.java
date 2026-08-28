package io.quarkus.it.vertx.openssl;

import javax.net.ssl.SSLSession;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.ReferenceCountedOpenSslEngine;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.internal.tcnative.SSL;
import io.vertx.core.http.HttpServerRequest;

@Path("/ssl")
public class SslEngineResource {

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello over TLS";
    }

    /**
     * Reports which TLS engine served the current connection, so a test can assert that the OpenSSL engine
     * (and not the JDK one) is in use. The session class is Netty's {@code OpenSslSession} for tcnative and
     * {@code sun.security.ssl.SSLSessionImpl} for SunJSSE.
     */
    @GET
    @Path("/engine")
    @Produces(MediaType.TEXT_PLAIN)
    public String engine(HttpServerRequest request) {
        SSLSession session = request.sslSession();
        if (session == null) {
            return "no-tls";
        }
        return session.getClass().getName() + " " + session.getProtocol() + " " + session.getCipherSuite();
    }

    /**
     * Runs the same probe as Vert.x {@code OpenSSLEngineOptions.isPqcAvailable()} but reports the failure instead
     * of swallowing it. Diagnostic aid for native-image support of the OpenSSL engine.
     */
    @GET
    @Path("/pqc-probe")
    @Produces(MediaType.TEXT_PLAIN)
    public String pqcProbe() {
        StringBuilder out = new StringBuilder();
        out.append("OpenSsl.isAvailable=").append(OpenSsl.isAvailable())
                .append(" version=").append(OpenSsl.versionString()).append('\n');
        try {
            SslContext ctx = SslContextBuilder.forClient()
                    .sslProvider(SslProvider.OPENSSL)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            out.append("context=").append(ctx.getClass().getName()).append('\n');
            SslHandler handler = ctx.newHandler(ByteBufAllocator.DEFAULT);
            out.append("engine=").append(handler.engine().getClass().getName()).append('\n');
            try {
                long sslPtr = ((ReferenceCountedOpenSslEngine) handler.engine()).sslPointer();
                out.append("setCurvesList(X25519MLKEM768)=").append(SSL.setCurvesList(sslPtr, "X25519MLKEM768"))
                        .append('\n');
            } finally {
                handler.engine().closeOutbound();
            }
        } catch (Throwable t) {
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            out.append("FAILED: ").append(sw);
        }
        return out.toString();
    }
}
