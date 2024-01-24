package io.quarkus.test.component;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.test.InjectMock;
import io.smallrye.common.annotation.Experimental;

/**
 * Registers the {@link QuarkusComponentTestExtension} that makes it easy to test Quarkus components.
 *
 * @see InjectMock
 * @see TestConfigProperty
 */
@Experimental("This feature is experimental and the API may change in the future")
@ExtendWith(QuarkusComponentTestExtension.class)
@Retention(RUNTIME)
@Target({ TYPE })
public @interface QuarkusComponentTest {

    /**
     * The set of additional components under test.
     * <p>
     * The initial set of components is derived from the test class. The types of all fields annotated with
     * {@link jakarta.inject.Inject} are considered the component types. Furthermore, all types of parameters of test methods
     * that are not annotated with {@link InjectMock} or {@link SkipInject} are also considered the component types.
     *
     * @return the components under test
     */
    Class<?>[] value() default {};

    /**
     * Indicates that the default values should be used for missing config properties.
     * <p>
     * If not used then a missing config property results in a test failure.
     * <p>
     * For primitives the default values as defined in the JLS are used. For any other type {@code null} is injected.
     *
     * @see QuarkusComponentTestExtensionBuilder#useDefaultConfigProperties()
     */
    boolean useDefaultConfigProperties() default false;

    /**
     * If set to {@code true} then all static nested classes are considered additional components under test.
     *
     * @see #value()
     */
    boolean addNestedClassesAsComponents() default true;

    /**
     * The ordinal of the config source used for all test config properties.
     *
     * @see QuarkusComponentTestExtensionBuilder#setConfigSourceOrdinal(int)
     */
    int configSourceOrdinal() default QuarkusComponentTestExtensionBuilder.DEFAULT_CONFIG_SOURCE_ORDINAL;

    /**
     * The additional annotation transformers.
     * <p>
     * The initial set includes the {@link JaxrsSingletonTransformer}.
     *
     * @see AnnotationsTransformer
     * @see QuarkusComponentTestExtensionBuilder#addAnnotationsTransformer(AnnotationsTransformer)
     */
    Class<? extends AnnotationsTransformer>[] annotationsTransformers() default {};

}
