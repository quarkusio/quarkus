package io.quarkus.observability.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.observability.test.support.ConfigEndpoint;
import io.quarkus.observability.test.support.ReloadEndpoint;
import io.quarkus.observability.test.utils.GrafanaClient;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Test hot reload, by changing the api path
 */
@DisabledOnOs(OS.WINDOWS)
@Tag("devmode")
@Disabled("2025-04-22 - This has been extremely flaky so disabling for now")
public class LgtmReloadTest extends LgtmTestHelper {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(ReloadEndpoint.class, ConfigEndpoint.class)
                            .addAsResource("application.properties",
                                    "application.properties"));

    @Override
    protected String grafanaEndpoint() {
        return GrafanaClient.endpoint();
    }

    @Test
    public void testReload() {
        poke("/reload");
        test.modifySourceFile(ReloadEndpoint.class, s -> s.replace("/reload", "/new"));
        poke("/new");
        test.modifyResourceFile("application.properties", s -> s.replace("timeout=PT3M", "timeout=PT4M"));
        poke("/new");
    }
}
