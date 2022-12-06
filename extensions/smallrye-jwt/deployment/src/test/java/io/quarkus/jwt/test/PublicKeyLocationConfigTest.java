package io.quarkus.jwt.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Level;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;

public class PublicKeyLocationConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addAsResource("publicKey.pem")
            .addClass(PublicKeyLocationBuildTimeConfigSourceFactory.class)
            .addAsServiceProvider(ConfigSourceFactory.class, PublicKeyLocationBuildTimeConfigSourceFactory.class))
            .overrideConfigKey("mp.jwt.verify.publickey.location", "publicKey.pem")
            .overrideConfigKey("quarkus.package.type", "native")
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                assertEquals("Cannot determine %s of mp.jwt.verify.publickey.location to register with the native image",
                        logRecords.get(0).getMessage());
            });

    @Inject
    SmallRyeConfig config;

    @Test
    void config() {
        assertEquals("publicKey.pem", config.getRawValue("mp.jwt.verify.publickey.location"));
    }
}
