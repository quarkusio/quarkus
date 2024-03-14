package io.quarkus.resteasy.reactive.qute.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.common.Assert;

import io.quarkus.test.QuarkusUnitTest;

public class TypeErrorTest3 {

    @RegisterExtension
    static final QuarkusUnitTest configError = new QuarkusUnitTest()
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
