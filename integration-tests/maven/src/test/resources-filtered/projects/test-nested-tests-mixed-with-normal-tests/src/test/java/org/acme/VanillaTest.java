package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.Config;

public class VanillaTest {

    private static Config config;

    @BeforeAll
    public static void createConfig() {
        // This is not a @QuarkusTest, so it runs without an application and without a configuration registered
        // for the classloader running it, but HelloResource requires one
        config = Config.getOrCreate();
    }

    @AfterAll
    public static void releaseConfig() {
        // Releases it from every classloader it was registered for, so it does not leak into other tests
        ConfigProviderResolver.instance().releaseConfig(config);
    }

    @Test
    public void test() {
        assertEquals(new HelloResource().hello(), "Hello from Quarkus REST via config");
    }

    @DisabledIf("isContinuousTesting")
    @Test
    public void testTCCL() {
        // This test is looking at internals, not externals, but I think it's a fair enough expectation
        // In continuous testing mode the normal tests get loaded with the deployment classloader, so we should not make assertions
        assertEquals(ClassLoader.getSystemClassLoader(), Thread.currentThread().getContextClassLoader());
    }

    public boolean isContinuousTesting() {
        return LaunchMode.current().isDevOrTest();
    }
}
