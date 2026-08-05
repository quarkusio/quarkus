package io.quarkus.arc.test.config;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ConfigPropertyInjectionWithoutInjectAnnotationTest {

    private static final String configValue = "someValue";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ConfigPropertyInjectionWithoutInjectAnnotationTest.class, SomeBeanUsingConfig.class)
                    .addAsResource(new StringAsset("something=" + configValue), "application.properties"));

    @Inject
    SomeBeanUsingConfig bean;

    @Test
    public void testConfigWasInjected() {
        Assertions.assertEquals(configValue, bean.getFoo());
    }
}
