package io.quarkus.resteasy.mutiny.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ContextInjector;

import io.quarkus.resteasy.mutiny.test.annotations.Async;
import io.smallrye.mutiny.Uni;

@Provider
public class MutinyInjector implements ContextInjector<Uni<Integer>, Integer> {

    @Override
    public Uni<Integer> resolve(Class<? extends Uni<Integer>> rawType, Type genericType,
            Annotation[] annotations) {
        boolean async = false;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == Async.class) {
                async = true;
            }
        }
        if (!async) {
            return Uni.createFrom().item(42);
        }
        return Uni.createFrom().emitter(emitter -> new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.fail(e);
                return;
            }
            emitter.complete(42);
        }).start());
    }

}
