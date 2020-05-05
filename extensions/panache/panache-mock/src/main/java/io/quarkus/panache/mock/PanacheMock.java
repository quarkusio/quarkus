package io.quarkus.panache.mock;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mockito.Mockito;
import org.mockito.internal.debugging.LocationImpl;
import org.mockito.internal.invocation.DefaultInvocationFactory;
import org.mockito.internal.invocation.InterceptedInvocation;
import org.mockito.internal.invocation.RealMethod;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.verification.VerificationMode;

public class PanacheMock {

    public static volatile boolean IsMockEnabled = false;

    private final static Map<Class<?>, Object> mocks = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T getMock(Class<T> klass) {
        return (T) mocks.get(klass);
    }

    public static Object[] getMocks(Class<?>... classes) {
        Object[] mocks = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            mocks[i] = getMock(classes[i]);
        }
        return mocks;
    }

    public static void mock(Class<?>... classes) {
        for (Class<?> klass : classes) {
            mocks.computeIfAbsent(klass, v -> Mockito.mock(klass));
        }
        IsMockEnabled = !mocks.isEmpty();
    }

    public static void reset() {
        mocks.clear();
        IsMockEnabled = false;
    }

    public static boolean isMocked(Class<?> klass) {
        return mocks.containsKey(klass);
    }

    public static Object mockMethod(Class<?> klass, String methodName, Class<?>[] parameterTypes, Object[] args)
            throws InvokeRealMethodException {
        try {
            Method invokedMethod = klass.getDeclaredMethod(methodName, parameterTypes);
            Object mock = getMock(klass);
            MockCreationSettings<?> settings = MockUtil.getMockSettings(mock);
            MyRealMethod myRealMethod = new MyRealMethod();
            InterceptedInvocation invocation = DefaultInvocationFactory.createInvocation(mock, invokedMethod, args,
                    myRealMethod, settings, new LocationImpl(new Throwable(), true));
            MockHandler<?> handler = MockUtil.getMockHandler(mock);
            return handler.handle(invocation);
        } catch (InvokeRealMethodException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    //
    // Delegating

    public static <T> T verify(Class<T> klass) {
        return Mockito.verify(getMock(klass));
    }

    public static <T> T verify(Class<T> klass, VerificationMode verificationMode) {
        return Mockito.verify(getMock(klass), verificationMode);
    }

    public static void verifyNoMoreInteractions(Class<?>... classes) {
        Mockito.verifyNoMoreInteractions(getMocks(classes));
    }

    public static void verifyNoInteractions(Class<?>... classes) {
        Mockito.verifyNoInteractions(getMocks(classes));
    }

    @SuppressWarnings("serial")
    public static class InvokeRealMethodException extends Exception {
    }

    @SuppressWarnings("serial")
    public static class MyRealMethod implements RealMethod {

        @Override
        public boolean isInvokable() {
            return true;
        }

        @Override
        public Object invoke() throws Throwable {
            throw new InvokeRealMethodException();
        }

    }

}
