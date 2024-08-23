package io.quarkus.resteasy.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.deployment.Capability;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

class UserFriendlyQuarkusRESTCapabilityCombinationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-rest-deployment", Version.getVersion())))
            .assertException(t -> {
                assertTrue(t.getMessage().contains("only one provider of the following capabilities"), t.getMessage());
                assertTrue(t.getMessage().contains("capability %s is provided by".formatted(Capability.REST)), t.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

}
