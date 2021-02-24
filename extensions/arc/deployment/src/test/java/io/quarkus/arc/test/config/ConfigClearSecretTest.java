package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ClearSecret;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SecretKeys;

public class ConfigClearSecretTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyExtensionConfig.class)
                    .addAsResource(new StringAsset(MyExtensionConfig.class.getName()), "META-INF/quarkus-config-roots.list")
                    .addAsResource(new StringAsset("quarkus.my-extension.secret=secret"), "application.properties"));

    @Inject
    Config config;

    // TODO - Injection also fails with the Security Exception. If SR Config Extension we allow to inject without calling doUnlocked
    // Still has the issue if the use prints the field directly. We could just allow injection to work if ClearSecret or other wrapper defined type for Secrets is used.
    //@ConfigProperty(name = "quarkus.my-extension.secret")
    //String secret;

    @Test
    void secret() {
        // Will throw exception if we try to lookup the key directly
        assertThrows(SecurityException.class,
                () -> config.getValue("quarkus.my-extension.secret", String.class),
                "SRCFG00024: Not allowed to access secret key quarkus.my-extension.secret");

        // Need to call do unlocked
        assertEquals("secret", SecretKeys.doUnlocked(
                () -> config.getValue("quarkus.my-extension.secret", String.class)));
    }

    @ConfigRoot
    public static class MyExtensionConfig {
        /**
         * Just a secret.
         */
        @ConfigItem
        public ClearSecret secret;
    }
}
