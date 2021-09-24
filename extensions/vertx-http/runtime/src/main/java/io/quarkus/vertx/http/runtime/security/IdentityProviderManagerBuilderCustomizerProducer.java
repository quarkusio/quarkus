package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.security.spi.runtime.IdentityProviderManagerBuilder;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@Singleton
public class IdentityProviderManagerBuilderCustomizerProducer {

    @Produces
    @Singleton
    public IdentityProviderManagerBuilder.Customizer produce() {
        return new IdentityProviderManagerBuilder.Customizer() {
            @Override
            public void customize(IdentityProviderManagerBuilder<?> builder) {
                builder.setEventLoopExecutorSupplier(new Supplier<>() {
                    @Override
                    public Executor get() {
                        Context context = Vertx.currentContext();
                        if (context == null) {
                            throw new IllegalStateException("Unable to determine Vert.x context");
                        }
                        if (!context.isEventLoopContext()) {
                            throw new IllegalStateException("Expected the Vert.x context to be an event-loop context");
                        }

                        return new Executor() {
                            @Override
                            public void execute(Runnable command) {
                                context.runOnContext(new Handler<>() {
                                    @Override
                                    public void handle(Void event) {
                                        command.run();
                                    }
                                });
                            }
                        };
                    }
                });
            }
        };
    }
}
