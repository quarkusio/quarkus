package io.quarkus.test.junit;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.TestInstance;

/**
 * Utility class that can be used to mock CDI normal scoped beans.
 *
 * This includes beans that are {@link jakarta.enterprise.context.ApplicationScoped} and
 * {@link jakarta.enterprise.context.RequestScoped}.
 *
 * To use this inject the bean into a test, and then invoke the mock
 * method with your mock.
 *
 * Mocks installed in {@link org.junit.jupiter.api.BeforeAll} will be present for every test,
 * while mocks installed within a test are cleared after the test has run. Note that you will
 * likely need to use {@link TestInstance.Lifecycle#PER_CLASS} to have a non-static before all method
 * that can access injected beans.
 *
 * Note that as the bean is replaced globally you cannot use parallel test execution, as this will
 * result in race conditions where mocks from one test are active in another.
 *
 */
public class QuarkusMock {

    /**
     * Installs a mock for a CDI normal scoped bean
     *
     * @param mock The mock object
     * @param instance The CDI normal scoped bean that was injected into your test
     * @param <T> The bean type
     */
    public static <T> void installMockForInstance(T mock, T instance) {
        //mock support does the actual work, but exposes other methods that are not part of the user API
        MockSupport.installMock(instance, mock);
    }

    /**
     * Installs a mock for a CDI normal scoped bean
     *
     * @param mock The mock object
     * @param instance The type of the CDI normal scoped bean to replace
     * @param qualifiers The CDI qualifiers of the bean to mock
     * @param <T> The bean type
     */
    public static <T> void installMockForType(T mock, Class<? super T> instance, Annotation... qualifiers) {
        //mock support does the actual work, but exposes other methods that are not part of the user API
        if (!instance.isAssignableFrom(mock.getClass())) {
            if (!(instance.getClass().getSuperclass().isAssignableFrom(mock.getClass()))) {
                throw new RuntimeException(mock
                        + " is not assignable to type " + instance.getClass().getSuperclass());
            }
        }
        MockSupport.installMock(CDI.current().select(instance, qualifiers).get(), mock);
    }
}
