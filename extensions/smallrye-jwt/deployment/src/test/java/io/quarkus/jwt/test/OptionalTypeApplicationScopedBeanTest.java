package io.quarkus.jwt.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class OptionalTypeApplicationScopedBeanTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(OptionalTypeApplicationScopedEndpoint.class))
            .assertException(t -> {
                assertTrue(t.getMessage().startsWith(
                        "java.util.Optional type can not be used to represent JWT claims in @Singleton or @ApplicationScoped beans, make the bean @RequestScoped"
                                + " or wrap this type with"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }
}
