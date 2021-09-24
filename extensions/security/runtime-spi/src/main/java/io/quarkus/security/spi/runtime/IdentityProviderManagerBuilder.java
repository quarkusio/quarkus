package io.quarkus.security.spi.runtime;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * This isn't a really useful interface in an SPI sense, it's only meant to allow the decoupling
 * of the creation of {@link io.quarkus.security.identity.IdentityProviderManager} from Vert.x
 * for Quarkus.
 */
public interface IdentityProviderManagerBuilder<T extends IdentityProviderManagerBuilder<?>> {

    T setEventLoopExecutorSupplier(Supplier<Executor> eventLoopExecutorSupplier);

    interface Customizer {

        void customize(IdentityProviderManagerBuilder<?> builder);
    }
}
