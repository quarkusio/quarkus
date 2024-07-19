package io.quarkus.test.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;

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
        System.out.println("HOLLY will ask CDI for current " + CDI.class.getClassLoader());
        // TODO #store

        Class<?> cdiClazz = null;
        try {
            cdiClazz = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(CDI.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Method m = null;
        Method select;
        try {
            m = cdiClazz.getMethod("current");
            select = Arrays.stream(cdiClazz.getMethods())
                    .filter(mm -> mm.getName().equals("select") && mm.getParameterTypes()[0].equals(Class.class)).findAny()
                    .get();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        //  CDI<Object> current = CDI.current();
        try {
            Object current = m.invoke(null);
            Object selected = select.invoke(current, instance, qualifiers);
            Class instanceClass = Thread.currentThread().getContextClassLoader().loadClass(Instance.class.getName());
            Method getMethod = instanceClass.getMethod("get");
            MockSupport.installMock(getMethod.invoke(selected), mock);

            //current.select(instance, qualifiers)
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Installs a mock for a CDI normal scoped bean by typeLiteral and qualifiers
     *
     * @param mock The mock object
     * @param typeLiteral TypeLiteral representing the required type
     * @param qualifiers The CDI qualifiers of the bean to mock
     * @param <T> The bean type
     */
    public static <T> void installMockForType(T mock, TypeLiteral<? super T> typeLiteral, Annotation... qualifiers) {
        MockSupport.installMock(CDI.current().select(typeLiteral, qualifiers).get(), mock);
    }
}
