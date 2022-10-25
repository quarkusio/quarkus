package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.reactive.messaging.providers.locals.LocalContextMetadata;
import io.vertx.core.Context;

@Interceptor
@DuplicatedContextConnectorFactory
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 5)
public class DuplicatedContextConnectorFactoryInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        if (ctx.getMethod().getName().equals("getPublisherBuilder")) {
            PublisherBuilder<Message<?>> result = (PublisherBuilder<Message<?>>) ctx.proceed();
            return result.map(message -> {
                Optional<LocalContextMetadata> metadata = message.getMetadata(LocalContextMetadata.class);
                if (metadata.isPresent()) {
                    Context context = metadata.get().context();
                    if (context != null && VertxContext.isDuplicatedContext(context)) {
                        VertxContextSafetyToggle.setContextSafe(context, true);
                    }
                }
                return message;
            });
        }

        return ctx.proceed();
    }
}
