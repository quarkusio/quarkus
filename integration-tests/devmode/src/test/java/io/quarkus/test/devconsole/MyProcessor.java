package io.quarkus.test.devconsole;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class MyProcessor {

    @Inject
    @Channel("processed")
    Instance<Publisher<String>> channel;

    public void init(@Observes Router router) {
        router.get().handler(rc -> {
            HttpServerResponse response = rc.response();
            channel.get().subscribe(new Subscriber<String>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    response.putHeader("Transfer-encoding", "chunked");
                    subscription.request(2);
                }

                @Override
                public void onNext(String s) {
                    response.write(s);
                }

                @Override
                public void onError(Throwable throwable) {
                    // ignore it.
                }

                @Override
                public void onComplete() {
                    response.end();
                }
            });
        });
    }

    @Incoming("input")
    @Outgoing("processed")
    @Broadcast
    public String process(String input) {
        return input.toUpperCase();
    }
}
