package io.quarkus.test.junit;

import java.lang.annotation.Annotation;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;

import org.junit.jupiter.api.TestInstance;

import io.quarkus.runtime.MockedThroughWrapper;

/**
 * Utility class that can be used to mock CDI normal scoped beans.
 * <p>
 * This includes beans that are {@link jakarta.enterprise.context.ApplicationScoped} and
 * {@link jakarta.enterprise.context.RequestScoped}.
 * <p>
 * To use this, inject the bean into a test and invoke {@link #installMockForInstance(Object, Object, Options)}.
 * Alternatively, invoke {@link #installMockForType(Object, TypeLiteral, Options, Annotation...)}.
 * <p>
 * Mocks installed in {@link org.junit.jupiter.api.BeforeAll} will be present for every test,
 * while mocks installed within a test are cleared after the test has run. Note that you will
 * likely need to use {@link TestInstance.Lifecycle#PER_CLASS} to have a non-static before all method
 * that can access injected beans.
 * <p>
 * Note that as the bean is replaced globally, you cannot use parallel test execution, as this will
 * result in race conditions where mocks from one test are active in another.
 */
public class QuarkusMock {

    /**
     * Installs the given {@code mock} for a CDI normal scoped bean identified by given {@code instance}.
     * Note that the given mock must be assignable to the bean class of the bean; it is <em>not enough</em>
     * for it to implement only some of the bean types.
     *
     * @param mock The mock object
     * @param instance The CDI normal scoped bean that was injected into your test
     * @param <T> The bean type
     */
    public static <T> void installMockForInstance(T mock, T instance) {
        //mock support does the actual work, but exposes other methods that are not part of the user API
        checkMockAssignableToInstance(mock, instance);
        MockSupport.installMock(instance, mock, new Options());
    }

    /**
     * Installs the given {@code mock} for a CDI normal scoped bean identified by given {@code instance}.
     * Note that the given mock must be assignable to the bean class of the bean; it is <em>not enough</em>
     * for it to implement only some of the bean types.
     *
     * @param mock The mock object
     * @param instance The CDI normal scoped bean that was injected into your test
     * @param <T> The bean type
     */
    public static <T> void installMockForInstance(T mock, T instance, Options options) {
        //mock support does the actual work, but exposes other methods that are not part of the user API
        checkMockAssignableToInstance(mock, instance);
        MockSupport.installMock(instance, mock, options);
    }

    /**
     * Installs the given {@code mock} for a CDI normal scoped bean of given {@code type} and with given {@code qualifiers}.
     * Note that the given mock must be assignable to the bean class of the looked up bean; it is <em>not enough</em>
     * for it to implement only some of the bean types.
     *
     * @param mock The mock object
     * @param type The type of the CDI normal scoped bean to replace
     * @param qualifiers The CDI qualifiers of the bean to mock
     * @param <T> The bean type
     */
    public static <T> void installMockForType(T mock, Class<? super T> type, Annotation... qualifiers) {
        Object instance = CDI.current().select(type, qualifiers).get();
        checkMockAssignableToInstance(mock, instance);
        MockSupport.installMock(instance, mock, new Options());
    }

    /**
     * Installs the given {@code mock} for a CDI normal scoped bean of given {@code type} and with given {@code qualifiers}.
     * Note that the given mock must be assignable to the bean class of the looked up bean; it is <em>not enough</em>
     * for it to implement only some of the bean types.
     *
     * @param mock The mock object
     * @param type The type of the CDI normal scoped bean to replace
     * @param qualifiers The CDI qualifiers of the bean to mock
     * @param <T> The bean type
     */
    public static <T> void installMockForType(T mock, TypeLiteral<? super T> type, Annotation... qualifiers) {
        Object instance = CDI.current().select(type, qualifiers).get();
        checkMockAssignableToInstance(mock, instance);
        MockSupport.installMock(instance, mock, new Options());
    }

    /**
     * Installs the given {@code mock} for a CDI normal scoped bean of given {@code type} and with given {@code qualifiers}.
     * Note that the given mock must be assignable to the bean class of the looked up bean; it is <em>not enough</em>
     * for it to implement only some of the bean types.
     *
     * @param mock The mock object
     * @param type TypeLiteral representing the required type
     * @param qualifiers The CDI qualifiers of the bean to mock
     * @param <T> The bean type
     */
    public static <T> void installMockForType(T mock, TypeLiteral<? super T> type, Options options,
            Annotation... qualifiers) {
        Object instance = CDI.current().select(type, qualifiers).get();
        checkMockAssignableToInstance(mock, instance);
        MockSupport.installMock(instance, mock, options);
    }

    private static <T> void checkMockAssignableToInstance(T mock, T instance) {
        Class<?> instanceClass;
        if (instance instanceof MockedThroughWrapper) {
            instanceClass = instance.getClass();
            if (instanceClass.getName().endsWith("_ClientProxy")) {
                // the condition is hideous, but we don't depend on ArC in this module
                instanceClass = instanceClass.getSuperclass();
            }
            // only generated by RestClient Reactive, which always implements exactly 1 interface
            instanceClass = instanceClass.getInterfaces()[0];
        } else if (instance instanceof Event<?>) {
            instanceClass = Event.class;
        } else if (instance.getClass().getName().endsWith("_ClientProxy")) {
            // the condition is hideous, but we don't depend on ArC in this module
            instanceClass = instance.getClass().getSuperclass();
        } else {
            throw new RuntimeException("Given instance (" + instance.getClass().getName()
                    + ") is not a client proxy, only normal scoped beans may be mocked");
        }
        Class<?> mockClass = mock.getClass();
        if (!instanceClass.isAssignableFrom(mockClass)) {
            throw new RuntimeException("Given mock, which is " + mockClass + ", is not assignable to " + instanceClass);
        }
    }

    /**
     * Allowed options for configuring mocked beans
     */
    public static class Options {

        private boolean mockObservers;

        public boolean isMockObservers() {
            return mockObservers;
        }

        public Options setMockObservers(boolean mockObservers) {
            this.mockObservers = mockObservers;
            return this;
        }
    }
}
