package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class PrimitiveTypeClaimApplicationScopedBeanTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(PrimitiveTypeClaimApplicationScopedEndpoint.class))
            .assertException(t -> {
                assertTrue(t.getMessage().startsWith(
                        "java.lang.String type can not be used to represent JWT claims in @Singleton or @ApplicationScoped beans, make the bean @RequestScoped"
                                + " or wrap this type with"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }
}
