package io.quarkus.resteasy.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Path("/in")
public class InputStreamResource {

    Timer timer = new Timer();

    public static final LinkedBlockingDeque<Throwable> THROWABLES = new LinkedBlockingDeque<>();

    @PreDestroy
    void stop() {
        timer.cancel();
    }

    @POST
    public String read(InputStream inputStream) throws IOException {
        try {
            byte[] buf = new byte[1024];
            int r;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((r = inputStream.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            THROWABLES.add(e);
            throw e;
        }
    }

    public void delayFilter(@Observes Router router) {
        router.route().order(Integer.MIN_VALUE).handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        event.next();
                    }
                }, 1000);
            }
        });
    }
}
