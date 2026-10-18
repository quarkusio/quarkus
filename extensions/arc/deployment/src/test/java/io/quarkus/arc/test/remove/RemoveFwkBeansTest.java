package io.quarkus.arc.test.remove;

import jakarta.enterprise.context.Dependent;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class RemoveFwkBeansTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UnusedBean.class)
                    .addAsResource(new StringAsset("quarkus.arc.remove-unused-beans=fwk"), "application.properties"));

    @Test
    public void unusedBeanNotRemoved() {
        Assertions.assertNotNull(Arc.container().instance(UnusedBean.class).get());
    }

    @Dependent
    public static class UnusedBean {

    }
}
