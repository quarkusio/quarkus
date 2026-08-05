package io.quarkus.resteasy.reactive.qute.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.constraint.Assert;

public class TypeErrorTest3 {

    @RegisterExtension
    static final QuarkusExtensionTest configError = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TypeErrorResource.class)
                    .addAsResource("templates/TypeErrorResource/typeError3.txt"))
            .assertException(t -> {
                t.printStackTrace();
                Assert.assertTrue(t.getMessage().contains("Incorrect expression: name.foo()"));
            });

    @Test
    public void emptyTest() {
    }
}
