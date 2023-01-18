package io.quarkus.smallrye.reactivemessaging.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.messaging.annotations.Channel;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class HttpFrontend {
    @Inject
    @Channel("processed")
    Instance<Publisher<String>> channel;

    @SuppressWarnings("SubscriberImplementation")
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
}
