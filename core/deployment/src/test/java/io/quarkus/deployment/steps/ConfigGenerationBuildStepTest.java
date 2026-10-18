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
 * Verifies that ConfigGenerationBuildStep#SMALLRYE_BUILT_IN_CONVERTER_TYPES matches SmallRye's own
 * built-in converters (Converters.ALL_CONVERTERS), minus any type Quarkus core also registers a
 * converter for via the Converter SPI. If this test fails, update the hardcoded set.
 */
class ConfigGenerationBuildStepTest {

    private static final String CONVERTER_SERVICES = "META-INF/services/" + Converter.class.getName();

    @Test
    @SuppressWarnings("unchecked")
    void smallRyeBuiltInConverterTypesIsCorrect() throws Exception {
        Set<String> expectedTypes = new TreeSet<>(smallRyeBuiltInConverterTypes());
        expectedTypes.removeAll(quarkusServiceLoadedConverterTypes());

        Field field = ConfigGenerationBuildStep.class.getDeclaredField("SMALLRYE_BUILT_IN_CONVERTER_TYPES");
        field.setAccessible(true);
        Set<String> hardcodedTypes = (Set<String>) field.get(null);

        assertThat(new TreeSet<>(hardcodedTypes)).isEqualTo(expectedTypes);
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
