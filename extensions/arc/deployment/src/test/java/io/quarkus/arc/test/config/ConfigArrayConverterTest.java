package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigArrayConverterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Configured.class)
                    .addAsResource(new StringAsset("foos=1,2,bar\nbools=true,false"), "application.properties"));

    @Inject
    Configured configured;

    @Test
    public void testFoo() {
        assertEquals(3, configured.foos.length);
        assertEquals("1", configured.foos[0]);
        assertEquals("2", configured.foos[1]);
        assertEquals("bar", configured.foos[2]);
        // Boolean[]
        assertEquals(2, configured.bools.length);
        assertEquals(false, configured.bools[1]);
        // boolean[]
        assertEquals(2, configured.boolsPrimitives.length);
        assertEquals(true, configured.boolsPrimitives[0]);
    }

    @Singleton
    static class Configured {

        @Deprecated
        @Inject
        @ConfigProperty(name = "foos")
        String[] foos;

        @Inject
        @ConfigProperty(name = "bools_primitives", defaultValue = "true,true")
        boolean[] boolsPrimitives;

        @Inject
        @ConfigProperty(name = "bools")
        Boolean[] bools;

    }

}
