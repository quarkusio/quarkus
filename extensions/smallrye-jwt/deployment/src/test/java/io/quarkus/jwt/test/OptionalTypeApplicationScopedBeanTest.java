package io.quarkus.jwt.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class OptionalTypeApplicationScopedBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(OptionalTypeApplicationScopedEndpoint.class))
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
