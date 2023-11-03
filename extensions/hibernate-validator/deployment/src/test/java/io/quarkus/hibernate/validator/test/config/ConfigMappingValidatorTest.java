package io.quarkus.hibernate.validator.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;

public class ConfigMappingValidatorTest {
    @RegisterExtension
    static final QuarkusUnitTest UNIT_TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("validator.server.host=localhost\n"), "application.properties"));

    @Inject
    @Valid
    Server server;

    @Test
    void valid() {
        assertEquals("localhost", server.host());
    }

    @Unremovable
    @ConfigMapping(prefix = "validator.server")
    public interface Server {
        @Size(min = 2, max = 10)
        String host();
    }
}
