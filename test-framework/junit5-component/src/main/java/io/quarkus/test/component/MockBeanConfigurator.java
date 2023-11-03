package io.quarkus.test.component;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Configures a mock of a bean.
 *
 * @param <T>
 * @see QuarkusComponentTestExtension#mock(Class)
 */
public interface MockBeanConfigurator<T> {

    MockBeanConfigurator<T> types(Class<?>... types);

    MockBeanConfigurator<T> types(java.lang.reflect.Type types);

    MockBeanConfigurator<T> qualifiers(Annotation... qualifiers);

    MockBeanConfigurator<T> scope(Class<? extends Annotation> scope);

    MockBeanConfigurator<T> name(String name);

    MockBeanConfigurator<T> alternative(boolean alternative);

    MockBeanConfigurator<T> priority(int priority);

    MockBeanConfigurator<T> defaultBean(boolean defaultBean);

    /**
     * Set the function used to create a new bean instance and register this configurator.
     *
     * @param create
     * @return the test extension
     */
    QuarkusComponentTestExtensionBuilder create(Function<SyntheticCreationalContext<T>, T> create);

    /**
     * A Mockito mock object created from the bean class is used as a bean instance.
     *
     * @return the test extension
     */
    QuarkusComponentTestExtensionBuilder createMockitoMock();

    /**
     * A Mockito mock object created from the bean class is used as a bean instance.
     *
     * @return the test extension
     */
    QuarkusComponentTestExtensionBuilder createMockitoMock(Consumer<T> mockInitializer);

}