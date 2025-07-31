package io.quarkus.stork;

import static org.assertj.core.api.Fail.fail;

import java.util.Arrays;
import java.util.logging.Level;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class UnknownRegistrarTypeConfigRegistrationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.INFO.intValue())
            .setForcedDependencies(
                    Arrays.asList(
                            Dependency.of("io.quarkus", "quarkus-smallrye-stork", Version.getVersion())))
            //                            Dependency.of("io.smallrye.stork", "stork-service-registration-consul", "2.7.3")))
            .assertException(throwable -> {
                Assertions.assertThat(throwable)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(
                                "Parameter type should be provided.");
            });

    @Test
    public void testBuildShouldFail() {
        fail("Should fail");
    }
}
