package io.quarkus.scheduler.test.devconsole;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.enterprise.event.Observes;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class NeverRunTask {

    private static final LinkedBlockingDeque<String> run = new LinkedBlockingDeque<>();

    public void setup(@Observes Router router) {
        router.route("/status").handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                try {
                    event.response().end(run.poll(10, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    event.fail(e);
                }
            }
        });
        router.route("/empty").handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                event.response().end(Boolean.toString(run.isEmpty()));
            }
        });
    }

    @Scheduled(every = "2h", delay = 2, delayUnit = TimeUnit.HOURS)
    public void run() {
        run.add("task ran");
    }

}
