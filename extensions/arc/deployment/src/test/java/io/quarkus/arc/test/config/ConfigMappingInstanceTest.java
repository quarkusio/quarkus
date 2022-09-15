package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithName;

public class ConfigMappingInstanceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("config.my.prop=1234\n"), "application.properties"));

    @ConfigMapping(prefix = "config")
    public interface MyConfigMapping {
        @WithName("my.prop")
        String myProp();
    }

    @ConfigProperties(prefix = "config")
    public static class MyConfigProperties {
        @ConfigProperty(name = "my.prop")
        String myProp;
    }

    @Inject
    SmallRyeConfig config;
    @Inject
    Instance<MyConfigMapping> myConfigMapping;
    @Inject
    @ConfigProperties
    Instance<MyConfigProperties> myConfigProperties;

    @Test
    void configMapping() {
        MyConfigMapping configMapping = config.getConfigMapping(MyConfigMapping.class);
        assertNotNull(configMapping);
        assertEquals("1234", configMapping.myProp());

        assertEquals("1234", myConfigMapping.get().myProp());
        assertEquals("1234", myConfigProperties.get().myProp);
    }
}
