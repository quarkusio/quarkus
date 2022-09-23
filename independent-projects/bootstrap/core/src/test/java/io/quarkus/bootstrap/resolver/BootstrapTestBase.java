package io.quarkus.bootstrap.resolver;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.QuarkusBootstrap;

public abstract class BootstrapTestBase extends ResolverSetupCleanup {

    @Test
    public void test() throws Exception {
        rebuild();
    }

    protected void rebuild() throws Exception {
        final QuarkusBootstrap.Builder bootstrap = initBootstrapBuilder();
        initProps(bootstrap);
        try {
            testBootstrap(bootstrap.build());
        } catch (Exception e) {
            assertError(e);
        }
    }

    protected abstract QuarkusBootstrap.Builder initBootstrapBuilder() throws Exception;

    protected void assertError(Exception e) throws Exception {
        throw e;
    }

    protected abstract void testBootstrap(QuarkusBootstrap creator) throws Exception;

    protected void initProps(QuarkusBootstrap.Builder builder) {
    }
}
