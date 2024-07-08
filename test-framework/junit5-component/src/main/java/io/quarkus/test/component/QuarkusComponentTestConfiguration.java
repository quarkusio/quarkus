package io.quarkus.test.component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.runtime.configuration.CharsetConverter;
import io.quarkus.runtime.configuration.CidrAddressConverter;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.runtime.configuration.InetAddressConverter;
import io.quarkus.runtime.configuration.InetSocketAddressConverter;
import io.quarkus.runtime.configuration.LocaleConverter;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.quarkus.runtime.configuration.PathConverter;
import io.quarkus.runtime.configuration.RegexConverter;
import io.quarkus.runtime.configuration.ZoneIdConverter;
import io.quarkus.runtime.logging.LevelConverter;
import io.quarkus.test.InjectMock;

class QuarkusComponentTestConfiguration {

    // As defined in /quarkus/core/runtime/src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter
    static final List<Converter<?>> DEFAULT_CONVERTERS = List.of(new InetSocketAddressConverter(),
            new CharsetConverter(),
            new CidrAddressConverter(),
            new InetAddressConverter(),
            new RegexConverter(),
            new PathConverter(),
            new DurationConverter(),
            new MemorySizeConverter(),
            new LocaleConverter(),
            new ZoneIdConverter(),
            new LevelConverter());

    static final QuarkusComponentTestConfiguration DEFAULT = new QuarkusComponentTestConfiguration(Map.of(), List.of(),
            List.of(), false, true, QuarkusComponentTestExtensionBuilder.DEFAULT_CONFIG_SOURCE_ORDINAL, List.of(),
            DEFAULT_CONVERTERS);

    private static final Logger LOG = Logger.getLogger(QuarkusComponentTestConfiguration.class);

    final Map<String, String> configProperties;
    final List<Class<?>> componentClasses;
    final List<MockBeanConfiguratorImpl<?>> mockConfigurators;
    final boolean useDefaultConfigProperties;
    final boolean addNestedClassesAsComponents;
    final int configSourceOrdinal;
    final List<AnnotationsTransformer> annotationsTransformers;
    final List<Converter<?>> configConverters;

    QuarkusComponentTestConfiguration(Map<String, String> configProperties, List<Class<?>> componentClasses,
            List<MockBeanConfiguratorImpl<?>> mockConfigurators, boolean useDefaultConfigProperties,
            boolean addNestedClassesAsComponents, int configSourceOrdinal,
            List<AnnotationsTransformer> annotationsTransformers, List<Converter<?>> configConverters) {
        this.configProperties = configProperties;
        this.componentClasses = componentClasses;
        this.mockConfigurators = mockConfigurators;
        this.useDefaultConfigProperties = useDefaultConfigProperties;
        this.addNestedClassesAsComponents = addNestedClassesAsComponents;
        this.configSourceOrdinal = configSourceOrdinal;
        this.annotationsTransformers = annotationsTransformers;
        this.configConverters = configConverters;
    }

    QuarkusComponentTestConfiguration update(Class<?> testClass) {
        Map<String, String> configProperties = new HashMap<>(this.configProperties);
        List<Class<?>> componentClasses = new ArrayList<>(this.componentClasses);
        boolean useDefaultConfigProperties = this.useDefaultConfigProperties;
        boolean addNestedClassesAsComponents = this.addNestedClassesAsComponents;
        int configSourceOrdinal = this.configSourceOrdinal;
        List<AnnotationsTransformer> annotationsTransformers = new ArrayList<>(this.annotationsTransformers);
        List<Converter<?>> configConverters = new ArrayList<>(this.configConverters);

        QuarkusComponentTest testAnnotation = testClass.getAnnotation(QuarkusComponentTest.class);
        if (testAnnotation != null) {
            Collections.addAll(componentClasses, testAnnotation.value());
            useDefaultConfigProperties = testAnnotation.useDefaultConfigProperties();
            addNestedClassesAsComponents = testAnnotation.addNestedClassesAsComponents();
            configSourceOrdinal = testAnnotation.configSourceOrdinal();
            Class<? extends AnnotationsTransformer>[] transformers = testAnnotation.annotationsTransformers();
            if (transformers.length > 0) {
                for (Class<? extends AnnotationsTransformer> transformerClass : transformers) {
                    try {
                        annotationsTransformers.add(transformerClass.getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to instantiate %s", transformerClass);
                    }
                }
            }
            Class<? extends Converter<?>>[] converters = testAnnotation.configConverters();
            if (converters.length > 0) {
                for (Class<? extends Converter<?>> converterClass : converters) {
                    try {
                        configConverters.add(converterClass.getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to instantiate %s", converterClass);
                    }
                }
            }
        }
        Class<?> current = testClass;
        while (current != null && current != Object.class) {
            // All fields annotated with @Inject represent component classes
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class) && !resolvesToBuiltinBean(field.getType())) {
                    componentClasses.add(field.getType());
                }
            }
            // All static nested classes declared on the test class are components
            if (addNestedClassesAsComponents) {
                for (Class<?> declaredClass : current.getDeclaredClasses()) {
                    if (Modifier.isStatic(declaredClass.getModifiers())) {
                        componentClasses.add(declaredClass);
                    }
                }
            }
            // All params of test methods but:
            // - not covered by built-in extensions
            // - not annotated with @InjectMock
            // - not annotated with @SkipInject
            for (Method method : current.getDeclaredMethods()) {
                if (QuarkusComponentTestExtension.isTestMethod(method)) {
                    for (Parameter param : method.getParameters()) {
                        if (QuarkusComponentTestExtension.BUILTIN_PARAMETER.test(param)
                                || param.isAnnotationPresent(InjectMock.class)
                                || param.isAnnotationPresent(SkipInject.class)) {
                            continue;
                        }
                        componentClasses.add(param.getType());
                    }
                }
            }
            current = current.getSuperclass();
        }

        List<TestConfigProperty> testConfigProperties = new ArrayList<>();
        Collections.addAll(testConfigProperties, testClass.getAnnotationsByType(TestConfigProperty.class));
        for (TestConfigProperty testConfigProperty : testConfigProperties) {
            configProperties.put(testConfigProperty.key(), testConfigProperty.value());
        }

        return new QuarkusComponentTestConfiguration(Map.copyOf(configProperties), List.copyOf(componentClasses),
                this.mockConfigurators,
                useDefaultConfigProperties, addNestedClassesAsComponents, configSourceOrdinal,
                List.copyOf(annotationsTransformers), List.copyOf(configConverters));
    }

    QuarkusComponentTestConfiguration update(Method testMethod) {
        Map<String, String> configProperties = new HashMap<>(this.configProperties);
        List<TestConfigProperty> testConfigProperties = new ArrayList<>();
        Collections.addAll(testConfigProperties, testMethod.getAnnotationsByType(TestConfigProperty.class));
        for (TestConfigProperty testConfigProperty : testConfigProperties) {
            configProperties.put(testConfigProperty.key(), testConfigProperty.value());
        }
        return new QuarkusComponentTestConfiguration(configProperties, componentClasses,
                mockConfigurators, useDefaultConfigProperties, addNestedClassesAsComponents, configSourceOrdinal,
                annotationsTransformers, configConverters);
    }

    private static boolean resolvesToBuiltinBean(Class<?> rawType) {
        return Provider.class.equals(rawType)
                || Instance.class.equals(rawType)
                || InjectableInstance.class.equals(rawType)
                || Event.class.equals(rawType)
                || BeanContainer.class.equals(rawType)
                || BeanManager.class.equals(rawType);
    }

}
