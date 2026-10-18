package io.quarkus.oidc.runtime;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;

public class BlockingTaskRunner<T> extends io.quarkus.oidc.common.runtime.BlockingTaskRunner<T>
        implements OidcRequestContext<T> {

    public BlockingTaskRunner() {
        super();
    }

    public BlockingTaskRunner(BlockingSecurityExecutor blockingExecutor) {
        super(blockingExecutor);
    }

}