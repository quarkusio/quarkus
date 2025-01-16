package io.quarkus.observability.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.observability.test.support.ConfigEndpoint;
import io.quarkus.observability.test.support.GrafanaClient;
import io.quarkus.observability.test.support.ReloadEndpoint;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Test hot reload, by changing the api path
 */
@DisabledOnOs(OS.WINDOWS)
@Tag("devmode")
public class LgtmReloadTest extends LgtmTestHelper {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(ReloadEndpoint.class, ConfigEndpoint.class));

    @Override
    protected String grafanaEndpoint() {
        return GrafanaClient.endpoint();
    }

    @Test
    public void testReload() {
        poke("/reload");
        test.modifySourceFile(ReloadEndpoint.class, s -> s.replace("/reload", "/new"));
        poke("/new");
    }
}
