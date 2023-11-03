package io.quarkus.mongodb.panache;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildException;
import io.quarkus.test.QuarkusUnitTest;

public class DuplicateIdWithParentTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.mongodb.devservices.enabled", "false")
            .setExpectedException(BuildException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(DuplicateIdWithParentEntity.class, DuplicateIdParentEntity.class));

    @Test
    void shouldThrow() {
        fail("A BuildException should have been thrown due to duplicate entity ID");
    }
}
