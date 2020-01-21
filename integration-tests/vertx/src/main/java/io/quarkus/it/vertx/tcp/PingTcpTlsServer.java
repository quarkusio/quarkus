package io.quarkus.it.vertx.tcp;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.net.NetServer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetServerOptions;

@ApplicationScoped
public class PingTcpTlsServer {

    @Inject
    Vertx vertx;

    public void init(@Observes StartupEvent ev) throws InterruptedException {
        NetServerOptions options = new NetServerOptions()
                .setHost("localhost")
                .setPort(4322)
                .setSsl(true)
                .setClientAuth(ClientAuth.REQUIRED)
                .setKeyStoreOptions(new JksOptions()
                        .setPath("src/main/resources/server-keystore.jks").setPassword("wibble"))
                .setTrustStoreOptions(
                        new JksOptions().setPath("src/main/resources/server-truststore.jks").setPassword("wibble"));
        NetServer server = vertx
                .createNetServer(options)
                .connectHandler(socket -> socket.handler(buffer -> {
                    String message = buffer.toString();
                    if (message.equalsIgnoreCase("ping")) {
                        socket.write("pong");
                    } else if (message.equalsIgnoreCase("pong")) {
                        socket.write("ping");
                    } else {
                        socket.close();
                    }
                }));
        CountDownLatch latch = new CountDownLatch(1);
        server.listen().thenAccept(s -> latch.countDown());
        latch.await();
    }

}
