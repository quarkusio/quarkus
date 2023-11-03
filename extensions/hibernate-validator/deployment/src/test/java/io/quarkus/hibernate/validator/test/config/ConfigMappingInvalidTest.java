package io.quarkus.hibernate.validator.test.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;

public class ConfigMappingInvalidTest {
    @RegisterExtension
    static final QuarkusUnitTest UNIT_TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("validator.server.host=localhost\n" +
                            "validator.server.services=redis,postgresql\n" +
                            "validator.hierarchy.number=1\n" +
                            "validator.repeatable.name=a"), "application.properties"));

    @Inject
    SmallRyeConfig config;

    @Test
    void invalid() {
        assertThrows(ConfigValidationException.class, () -> config.getConfigMapping(Server.class),
                "validator.server.host must be less than or equal to 3");
    }

    @Test
    @Disabled("Requires https://github.com/smallrye/smallrye-config/pull/923")
    void invalidHierarchy() {
        assertThrows(ConfigValidationException.class, () -> config.getConfigMapping(Child.class),
                "validator.hierarchy.number must be greater than or equal to 10");
    }

    @Test
    void repeatable() {
        assertThrows(ConfigValidationException.class, () -> config.getConfigMapping(Repeatable.class));
    }

    @Unremovable
    @ConfigMapping(prefix = "validator.server")
    public interface Server {
        @Max(3)
        String host();

        List<@NotEmpty String> services();
    }

    public interface Parent {
        @Min(10)
        Integer number();
    }

    @Unremovable
    @ConfigMapping(prefix = "validator.hierarchy")
    public interface Child extends Parent {

    }

    @Unremovable
    @ConfigMapping(prefix = "validator.repeatable")
    public interface Repeatable {
        @Size(max = 10)
        @Size(min = 2)
        String name();
    }
}
