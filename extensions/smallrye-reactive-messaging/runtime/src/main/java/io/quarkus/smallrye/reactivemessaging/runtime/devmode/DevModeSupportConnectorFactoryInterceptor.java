package io.quarkus.smallrye.reactivemessaging.runtime.devmode;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Interceptor
@DevModeSupportConnectorFactory
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 10)
public class DevModeSupportConnectorFactoryInterceptor {
    private static volatile Supplier<CompletableFuture<Boolean>> onMessage;

    static void register(Supplier<CompletableFuture<Boolean>> onMessage) {
        DevModeSupportConnectorFactoryInterceptor.onMessage = onMessage;
    }

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        if (onMessage == null) {
            return ctx.proceed();
        }

        if (ctx.getMethod().getName().equals("getPublisherBuilder")) {
            PublisherBuilder<Message<?>> result = (PublisherBuilder<Message<?>>) ctx.proceed();
            return result.flatMapCompletionStage(msg -> {
                CompletableFuture<Message<?>> future = new CompletableFuture<>();
                onMessage.get().whenComplete((restarted, error) -> {
                    if (!restarted) {
                        // if restarted, a new stream is already running,
                        // no point in emitting an event to the old stream
                        future.complete(msg);
                    }
                });
                return future;
            });
        }

        if (ctx.getMethod().getName().equals("getSubscriberBuilder")) {
            SubscriberBuilder<Message<?>, Void> result = (SubscriberBuilder<Message<?>, Void>) ctx.proceed();
            return ReactiveStreams.fromSubscriber(new Subscriber<Message<?>>() {
                private Subscriber<Message<?>> subscriber;

                @Override
                public void onSubscribe(Subscription s) {
                    subscriber = result.build();
                    subscriber.onSubscribe(s);
                }

                @Override
                public void onNext(Message<?> o) {
                    subscriber.onNext(o);
                    onMessage.get();
                }

                @Override
                public void onError(Throwable t) {
                    subscriber.onError(t);
                    onMessage.get();
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                    onMessage.get();
                }
            });
        }

        return ctx.proceed();
    }
}
