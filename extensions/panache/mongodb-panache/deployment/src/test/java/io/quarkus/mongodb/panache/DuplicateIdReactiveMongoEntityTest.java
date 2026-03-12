package io.quarkus.mongodb.panache;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildException;
import io.quarkus.test.QuarkusExtensionTest;

public class DuplicateIdReactiveMongoEntityTest {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.mongodb.devservices.enabled", "false")
            .setExpectedException(BuildException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(DuplicateIdReactiveMongoEntity.class));

    @Test
    void shouldThrow() {
        fail("A BuildException should have been thrown due to duplicate entity ID");
    }

}
