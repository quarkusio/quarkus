package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultMultiPathConfigITCase {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-multi-path.properties", "application.properties"));

    @Test
    public void defaultPath() {
        Config config = ConfigProviderResolver.instance().getConfig();
        assertEquals("red", config.getValue("color", String.class));
        assertEquals("XL", config.getValue("size", String.class));
        assertEquals("3", config.getValue("weight", String.class));
    }

    @Test
    public void prefixPath() {
        Config config = ConfigProviderResolver.instance().getConfig();
        assertEquals("green", config.getValue("singer.color", String.class));
        assertEquals("paul", config.getValue("singer.firstname", String.class));
        assertEquals("simon", config.getValue("singer.lastname", String.class));
        assertEquals("78", config.getValue("singer.age", String.class));
    }
}
