package io.quarkus.it.hibernate.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.validator.runtime.HibernateBeanValidationConfigValidator;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValidationException.Problem;
import io.smallrye.config.ConfigValidator;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithDefault;

@QuarkusTest
@TestProfile(ConfigMappingStartupValidatorTest.Profile.class)
public class ConfigMappingStartupValidatorTest {

    @ConfigMapping(prefix = "config")
    public static interface ConfigWithValidation {
        public static final String CONFIG_WITH_VALIDATION_VALUE_MUST_BE_D_3 = "ConfigWithValidation.value() must be \"-\\d{3}\"";

        @WithDefault("invalid-value")
        @Pattern(regexp = "-\\d{3}", message = CONFIG_WITH_VALIDATION_VALUE_MUST_BE_D_3)
        String value();
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public boolean disableGlobalTestResources() {
            return false;
        }
    }

    private static SmallRyeConfig smallryeConfig;
    private static Map<Class<?>, Exception> suppressedConfigValidatorExceptions;

    @Inject
    ConfigWithValidation config;

    @BeforeAll
    public static void doBefore() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.withMapping(ConfigWithValidation.class).setAddDefaultSources(true)
                .withValidator(new ConfigValidator() {
                    final ConfigValidator base = new HibernateBeanValidationConfigValidator();

                    @Override
                    public void validateMapping(Class<?> mappingClass, String prefix, Object mappingObject)
                            throws ConfigValidationException {
                        try {
                            base.validateMapping(mappingClass, prefix, mappingObject);
                        } catch (Exception e) {
                            suppressedConfigValidatorExceptions.put(mappingClass, e);
                        }
                    }
                });
        QuarkusConfigFactory.setConfig(smallryeConfig = builder.build());
        Config conf = ConfigProvider.getConfig();
        if (conf != smallryeConfig) {
            ConfigProviderResolver cpr = ConfigProviderResolver.instance();
            cpr.releaseConfig(conf);
            ConfigProvider.getConfig();
        }
        suppressedConfigValidatorExceptions = new HashMap<>();
    }

    @AfterAll
    public static void doAfter() {
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        cpr.releaseConfig(smallryeConfig);
        smallryeConfig = null;
        suppressedConfigValidatorExceptions = null;
    }

    @Test
    public void test() {
        assertEquals("invalid-value", config.value());
        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> {
            throw suppressedConfigValidatorExceptions.get(ConfigWithValidation.class);
        });
        assertEquals(1, ex.getProblemCount());
        final Problem problem = ex.getProblem(0);
        assertEquals("config.value " + ConfigWithValidation.CONFIG_WITH_VALIDATION_VALUE_MUST_BE_D_3, problem.getMessage());
    }
}
