package io.quarkus.qute.resteasy.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TypeErrorTest3 {

    @RegisterExtension
    static final QuarkusUnitTest configError = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TypeErrorResource.class)
                    .addAsResource("templates/TypeErrorResource/typeError3.txt"))
            .assertException(t -> {
                assertTrue(t.getMessage().contains("Incorrect expression"));
                assertTrue(t.getMessage().contains("name.foo()"));
            });

    @Test
    public void emptyTest() {
    }
}
