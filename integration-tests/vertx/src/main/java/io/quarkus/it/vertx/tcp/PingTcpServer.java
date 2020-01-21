package io.quarkus.it.vertx.tcp;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

@ApplicationScoped
public class PingTcpServer {

    @Inject
    Vertx vertx;

    public void init(@Observes StartupEvent ev) throws InterruptedException {
        NetServerOptions options = new NetServerOptions()
                .setHost("localhost")
                .setPort(4321);
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
