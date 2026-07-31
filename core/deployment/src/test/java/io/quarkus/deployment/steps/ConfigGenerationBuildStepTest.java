package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.util.ServiceUtil;
import io.smallrye.config.Converters;

/**
 * This test exists to verify that the constant fields
 * ConfigGenerationBuildStep#SMALLRYE_BUILT_IN_CONVERTER_TYPES and
 * ConfigGenerationBuildStep#QUARKUS_CONVERTER_SPI_TYPES
 * are aligned with the content of the Smallrye Config version we depend on and the converters
 * Quarkus core itself registers, namely:
 * - QUARKUS_CONVERTER_SPI_TYPES must match the target types of all converters provided by Quarkus by
 * default (listed in
 * quarkus/core/runtime/src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter)
 * - SMALLRYE_BUILT_IN_CONVERTER_TYPES must match all converters included by default in Smallrye Config
 * (field ALL_CONVERTERS in class io.smallrye.config.Converters), minus the types already covered by
 * QUARKUS_CONVERTER_SPI_TYPES
 * If this test fails, it implies the hardcoded lists need to be updated.
 */
class ConfigGenerationBuildStepTest {

    private static final String CONVERTER_SERVICES = "META-INF/services/" + Converter.class.getName();

    @Test
    @SuppressWarnings("unchecked")
    void converterTypeSetsMatchSmallRyeConfigAndQuarkusCore() throws Exception {
        Set<String> smallRyeTypes = smallRyeBuiltInConverterTypes();
        Set<String> quarkusTypes = quarkusServiceLoadedConverterTypes();

        Field quarkusSpiField = ConfigGenerationBuildStep.class.getDeclaredField("QUARKUS_CONVERTER_SPI_TYPES");
        quarkusSpiField.setAccessible(true);
        Set<String> hardcodedQuarkusSpiTypes = (Set<String>) quarkusSpiField.get(null);

        assertThat(new TreeSet<>(hardcodedQuarkusSpiTypes))
                .as("QUARKUS_CONVERTER_SPI_TYPES must match the target types of the converters provided by "
                        + "Quarkus core - update the hardcoded set if this fails")
                .isEqualTo(new TreeSet<>(quarkusTypes));

        Field smallRyeBuiltInField = ConfigGenerationBuildStep.class.getDeclaredField("SMALLRYE_BUILT_IN_CONVERTER_TYPES");
        smallRyeBuiltInField.setAccessible(true);
        Set<String> hardcodedSmallRyeTypes = (Set<String>) smallRyeBuiltInField.get(null);

        Set<String> expectedSmallRyeOnlyTypes = new TreeSet<>(smallRyeTypes);
        expectedSmallRyeOnlyTypes.removeAll(quarkusTypes);

        assertThat(new TreeSet<>(hardcodedSmallRyeTypes))
                .as("SMALLRYE_BUILT_IN_CONVERTER_TYPES must match SmallRye Converters.ALL_CONVERTERS types "
                        + "that are not also registered by Quarkus core via the Converter SPI - update the "
                        + "hardcoded set if this fails")
                .isEqualTo(expectedSmallRyeOnlyTypes);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> smallRyeBuiltInConverterTypes() throws Exception {
        Field smallryeField = Converters.class.getDeclaredField("ALL_CONVERTERS");
        smallryeField.setAccessible(true);
        Map<Type, ?> allConverters = (Map<Type, ?>) smallryeField.get(null);
        return allConverters.keySet().stream()
                .filter(t -> t instanceof Class<?>)
                .map(t -> ((Class<?>) t).getName())
                .collect(Collectors.toSet());
    }

    /**
     * The converter target types Quarkus core registers via the {@link Converter} SPI. Only the Quarkus core
     * runtime declares such a service file on this module's test classpath, so this resolves to the
     * contents of {@code core/runtime/src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter}.
     */
    private static Set<String> quarkusServiceLoadedConverterTypes() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> converterNames = ServiceUtil.classNamesNamedIn(classLoader, CONVERTER_SERVICES);
        assertThat(converterNames)
                .as("The Quarkus core Converter service file should be on the test classpath")
                .isNotEmpty();

        Set<String> types = new TreeSet<>();
        for (String converterName : converterNames) {
            Class<?> converterClass = Class.forName(converterName, false, classLoader);
            Type converterType = Converters.getConverterType(converterClass);
            assertThat(converterType)
                    .as("Unable to resolve the converted type of " + converterName)
                    .isInstanceOf(Class.class);
            types.add(((Class<?>) converterType).getName());
        }
        return types;
    }
}
