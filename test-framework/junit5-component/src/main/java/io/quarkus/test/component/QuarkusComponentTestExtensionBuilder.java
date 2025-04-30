package io.quarkus.test.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Convenient builder for {@link QuarkusComponentTestExtension}.
 */
public class QuarkusComponentTestExtensionBuilder {

    /**
     * By default, test config properties take precedence over system properties (400), ENV variables (300) and
     * application.properties (250)
     *
     * @see #setConfigSourceOrdinal(int)
     */
    public static final int DEFAULT_CONFIG_SOURCE_ORDINAL = 500;

    private final Map<String, String> configProperties = new HashMap<>();
    private final Set<Class<?>> componentClasses = new HashSet<>();
    private final List<MockBeanConfiguratorImpl<?>> mockConfigurators = new ArrayList<>();
    private final List<AnnotationsTransformer> annotationsTransformers = new ArrayList<>();
    private final List<Converter<?>> configConverters = new ArrayList<>();
    private boolean useDefaultConfigProperties = false;
    private boolean addNestedClassesAsComponents = true;
    private int configSourceOrdinal = QuarkusComponentTestExtensionBuilder.DEFAULT_CONFIG_SOURCE_ORDINAL;
    private Consumer<SmallRyeConfigBuilder> configBuilderCustomizer;
    private boolean buildShouldFail;

    /**
     * The initial set of components under test is derived from the test class. The types of all fields annotated with
     * {@link jakarta.inject.Inject} are considered the component types.
     *
     *
     * @param componentClasses
     * @return self
     * @see #ignoreNestedClasses()
     */
    public QuarkusComponentTestExtensionBuilder addComponentClasses(Class<?>... componentClasses) {
        Collections.addAll(this.componentClasses, componentClasses);
        return this;
    }

    /**
     * Set a configuration property for the test.
     *
     * @param key
     * @param value
     * @return self
     */
    public QuarkusComponentTestExtensionBuilder configProperty(String key, String value) {
        configProperties.put(key, value);
        return this;
    }

    /**
     * Use the default values for missing config properties. By default, a missing config property results in a test
     * failure.
     * <p>
     * For primitives the default values as defined in the JLS are used. For any other type {@code null} is injected.
     *
     * @return self
     */
    public QuarkusComponentTestExtensionBuilder useDefaultConfigProperties() {
        useDefaultConfigProperties = true;
        return this;
    }

    /**
     * Ignore the static nested classes declared on the test class.
     * <p>
     * By default, all static nested classes declared on the test class are added to the set of additional components under
     * test.
     *
     * @return self
     */
    public QuarkusComponentTestExtensionBuilder ignoreNestedClasses() {
        addNestedClassesAsComponents = false;
        return this;
    }

    /**
     * Set the ordinal of the config source used for all test config properties. By default,
     * {@value #DEFAULT_CONFIG_SOURCE_ORDINAL} is used.
     *
     * @param val
     * @return self
     */
    public QuarkusComponentTestExtensionBuilder setConfigSourceOrdinal(int val) {
        configSourceOrdinal = val;
        return this;
    }

    /**
     * Add an additional {@link AnnotationsTransformer}.
     *
     * @param transformer
     * @return self
     */
    public QuarkusComponentTestExtensionBuilder addAnnotationsTransformer(AnnotationsTransformer transformer) {
        annotationsTransformers.add(transformer);
        return this;
    }

    /**
     * Add an additional {@link Converter}. By default, the Quarkus-specific converters are registered.
     *
     * @param converter
     * @return self
     * @see #setConfigBuilderCustomizer(Consumer)
     */
    public QuarkusComponentTestExtensionBuilder addConverter(Converter<?> converter) {
        configConverters.add(converter);
        return this;
    }

    /**
     * Set the {@link SmallRyeConfigBuilder} customizer.
     * <p>
     * The customizer can affect the configuration of a test method and should be used with caution.
     *
     * @param customizer
     * @return self
     */
    public QuarkusComponentTestExtensionBuilder setConfigBuilderCustomizer(Consumer<SmallRyeConfigBuilder> customizer) {
        this.configBuilderCustomizer = customizer;
        return this;
    }

    /**
     * Configure a new mock of a bean.
     * <p>
     * Note that a mock is created automatically for all unsatisfied dependencies in the test. This API provides full control
     * over the bean attributes. The default values are derived from the bean class.
     *
     * @param beanClass
     * @return a new mock bean configurator
     * @see MockBeanConfigurator#create(Function)
     */
    public <T> MockBeanConfigurator<T> mock(Class<T> beanClass) {
        return new MockBeanConfiguratorImpl<>(this, beanClass);
    }

    QuarkusComponentTestExtensionBuilder buildShouldFail() {
        this.buildShouldFail = true;
        return this;
    }

    /**
     *
     * @return a new extension instance
     */
    public QuarkusComponentTestExtension build() {
        List<Converter<?>> converters;
        if (configConverters.isEmpty()) {
            converters = QuarkusComponentTestConfiguration.DEFAULT_CONVERTERS;
        } else {
            converters = new ArrayList<>(QuarkusComponentTestConfiguration.DEFAULT_CONVERTERS);
            converters.addAll(configConverters);
            converters = List.copyOf(converters);
        }
        return new QuarkusComponentTestExtension(new QuarkusComponentTestConfiguration(Map.copyOf(configProperties),
                Set.copyOf(componentClasses), List.copyOf(mockConfigurators), useDefaultConfigProperties,
                addNestedClassesAsComponents, configSourceOrdinal,
                List.copyOf(annotationsTransformers), converters, configBuilderCustomizer), buildShouldFail);
    }

    void registerMockBean(MockBeanConfiguratorImpl<?> mock) {
        this.mockConfigurators.add(mock);
    }

}