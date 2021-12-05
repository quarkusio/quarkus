package io.quarkus.arc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.wildfly.common.Assert.assertFalse;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.QuarkusUnitTest;

public class SimpleBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("simpleBean.baz=1"), "application.properties"));

    @Inject
    SimpleBean simpleBean;

    @Inject
    LaunchMode launchMode;

    @Test
    public void testSimpleBean() {
        assertNotNull(simpleBean.getStartupEvent());
        assertEquals(SimpleBean.DEFAULT, simpleBean.getFoo());
        assertFalse(simpleBean.getFooOptional().isPresent());
        assertEquals("1", simpleBean.getBazOptional().get());
        assertEquals("1", simpleBean.getBazProvider().get());
    }

    @Test
    public void testLaunchModeInjection() {
        assertEquals(LaunchMode.TEST, launchMode);
    }

}
