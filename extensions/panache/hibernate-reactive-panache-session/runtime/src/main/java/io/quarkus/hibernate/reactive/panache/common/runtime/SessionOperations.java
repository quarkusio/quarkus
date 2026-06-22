package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Transaction;

import io.smallrye.mutiny.Uni;

/**
 * Facade used by Hibernate Reactive Panache and by the Hibernate processor generated metamodel.
 * <p>
 * The actual implementation is provided by {@link SessionOperationsDelegate} when the Hibernate Reactive extension is
 * present.
 */
public final class SessionOperations {

    private static final String DELEGATE_CLASS_NAME = "io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperationsDelegate";

    private static final String MISSING_HIBERNATE_REACTIVE = "Reactive Panache types require the Hibernate Reactive extension. "
            + "Add the 'quarkus-hibernate-reactive' extension and a reactive driver extension "
            + "(for example 'quarkus-reactive-pg-client') to your project dependencies.";

    private SessionOperations() {
    }

    public static <T> Uni<T> withTransaction(Supplier<Uni<T>> work) {
        return invokeDelegate("withTransaction", work);
    }

    public static <T> Uni<T> withTransaction(String persistenceUnitName, Supplier<Uni<T>> work) {
        return invokeDelegate("withTransaction", persistenceUnitName, work);
    }

    public static <T> Uni<T> withTransaction(Function<Transaction, Uni<T>> work) {
        return invokeDelegate("withTransaction", work);
    }

    public static <T> Uni<T> withStatelessTransaction(Supplier<Uni<T>> work) {
        return invokeDelegate("withStatelessTransaction", work);
    }

    public static <T> Uni<T> withStatelessTransaction(String persistenceUnitName, Supplier<Uni<T>> work) {
        return invokeDelegate("withStatelessTransaction", persistenceUnitName, work);
    }

    public static <T> Uni<T> withStatelessTransaction(Function<Transaction, Uni<T>> work) {
        return invokeDelegate("withStatelessTransaction", work);
    }

    public static <T> Uni<T> withSession(String persistenceUnitName, Function<Mutiny.Session, Uni<T>> work) {
        return invokeDelegate("withSession", persistenceUnitName, work);
    }

    public static <T> Uni<T> withSession(Function<Mutiny.Session, Uni<T>> work) {
        return invokeDelegate("withSession", work);
    }

    public static <T> Uni<T> withStatelessSession(String persistenceUnitName, Function<Mutiny.StatelessSession, Uni<T>> work) {
        return invokeDelegate("withStatelessSession", persistenceUnitName, work);
    }

    public static <T> Uni<T> withStatelessSession(Function<Mutiny.StatelessSession, Uni<T>> work) {
        return invokeDelegate("withStatelessSession", work);
    }

    public static Uni<Mutiny.Session> getSession() {
        return invokeDelegate("getSession");
    }

    public static Uni<Mutiny.Session> getSession(String persistenceUnitName) {
        return invokeDelegate("getSession", persistenceUnitName);
    }

    public static Uni<Mutiny.StatelessSession> getStatelessSession() {
        return invokeDelegate("getStatelessSession");
    }

    public static Uni<Mutiny.StatelessSession> getStatelessSession(String persistenceUnitName) {
        return invokeDelegate("getStatelessSession", persistenceUnitName);
    }

    public static Mutiny.Session getCurrentSession(String persistenceUnitName) {
        return invokeDelegate("getCurrentSession", persistenceUnitName);
    }

    public static Mutiny.StatelessSession getCurrentStatelessSession(String persistenceUnitName) {
        return invokeDelegate("getCurrentStatelessSession", persistenceUnitName);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeDelegate(String methodName, Object... args) {
        try {
            Class<?> delegateClass = Class.forName(DELEGATE_CLASS_NAME, true,
                    Thread.currentThread().getContextClassLoader());
            Class<?>[] parameterTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i].getClass();
                // unwrap lambdas - use declared interfaces
                if (parameterTypes[i].getName().contains("$$Lambda")) {
                    for (Class<?> iface : parameterTypes[i].getInterfaces()) {
                        if (iface != java.io.Serializable.class) {
                            parameterTypes[i] = iface;
                            break;
                        }
                    }
                }
            }
            Method method = delegateClass.getMethod(methodName, parameterTypes);
            return (T) method.invoke(null, args);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(MISSING_HIBERNATE_REACTIVE, e);
        } catch (NoSuchMethodException e) {
            try {
                Class<?> delegateClass = Class.forName(DELEGATE_CLASS_NAME, true,
                        Thread.currentThread().getContextClassLoader());
                Method method = findMethod(delegateClass, methodName, args);
                return (T) method.invoke(null, args);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(MISSING_HIBERNATE_REACTIVE, ex);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(MISSING_HIBERNATE_REACTIVE, e);
        }
    }

    private static Method findMethod(Class<?> delegateClass, String methodName, Object[] args)
            throws NoSuchMethodException {
        for (Method method : delegateClass.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < args.length; i++) {
                if (!isCompatible(parameterTypes[i], args[i])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return method;
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static boolean isCompatible(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }
        if (parameterType.isInstance(arg)) {
            return true;
        }
        for (Class<?> iface : arg.getClass().getInterfaces()) {
            if (parameterType.isAssignableFrom(iface)) {
                return true;
            }
        }
        return false;
    }
}
