package io.quarkus.runtime.execution;

import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.common.Assert;

/**
 * A startup handler that lazily acquires a delegate handler from a supplier or function.
 */
public final class SupplierExecutionHandler implements ExecutionHandler, Serializable {
    private static final long serialVersionUID = 85967996322605351L;

    private final Function<? super ExecutionContext, ? extends ExecutionHandler> function;

    /**
     * Construct a new instance from a class name.
     *
     * @param handlerClassName the class name
     * @deprecated This constructor will eventually be replaced once there is a build-time class loader for application
     *             classes.
     */
    @Deprecated
    public SupplierExecutionHandler(String handlerClassName) {
        this(classNameSupplierOf(Assert.checkNotNullParam("handlerClassName", handlerClassName)));
    }

    /**
     * Construct a new instance from a class with a no-argument constructor.
     *
     * @param handlerClass the class to use (must not be {@code null})
     */
    public SupplierExecutionHandler(Class<? extends ExecutionHandler> handlerClass) {
        this(constructorOf(Assert.checkNotNullParam("handlerClass", handlerClass)));
    }

    /**
     * Construct a new instance from a no-argument constructor.
     *
     * @param handlerCtor the constructor to call (must not be {@code null})
     */
    public SupplierExecutionHandler(Constructor<? extends ExecutionHandler> handlerCtor) {
        this(supplierOf(Assert.checkNotNullParam("handlerCtor", handlerCtor)));
    }

    /**
     * Construct a new instance. If the given supplier returns {@code null}, then
     * a {@code NullPointerException} will result at startup.
     *
     * @param supplier the supplier to use (must not be {@code null})
     */
    public SupplierExecutionHandler(final Supplier<? extends ExecutionHandler> supplier) {
        this(functionOf(Assert.checkNotNullParam("supplier", supplier)));
    }

    /**
     * Construct a new instance. If the given function returns {@code null}, then
     * a {@code NullPointerException} will result at startup.
     *
     * @param function the function to use (must not be {@code null})
     */
    public SupplierExecutionHandler(final Function<? super ExecutionContext, ? extends ExecutionHandler> function) {
        this.function = Assert.checkNotNullParam("function", function);
    }

    public int run(final ExecutionChain chain, final ExecutionContext context) throws Exception {
        return function.apply(context).run(chain, context);
    }

    private static Supplier<? extends ExecutionHandler> classNameSupplierOf(final String handlerClassName) {
        return new NameSupp(handlerClassName);
    }

    private static Constructor<? extends ExecutionHandler> constructorOf(final Class<? extends ExecutionHandler> handlerClass) {
        try {
            return handlerClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw toError(e);
        }
    }

    static Supplier<? extends ExecutionHandler> supplierOf(final Constructor<? extends ExecutionHandler> handlerCtor) {
        return new CtorSupp(handlerCtor);
    }

    static Function<? super ExecutionContext, ? extends ExecutionHandler> functionOf(
            final Supplier<? extends ExecutionHandler> supplier) {
        return new SuppFunc(supplier);
    }

    static InstantiationError toError(final InstantiationException e) {
        final InstantiationError error = new InstantiationError(e.getMessage());
        error.setStackTrace(e.getStackTrace());
        return error;
    }

    static IllegalAccessError toError(final IllegalAccessException e) {
        final IllegalAccessError error = new IllegalAccessError(e.getMessage());
        error.setStackTrace(e.getStackTrace());
        return error;
    }

    static NoSuchMethodError toError(final NoSuchMethodException e) {
        final NoSuchMethodError error = new NoSuchMethodError(e.getMessage());
        error.setStackTrace(e.getStackTrace());
        return error;
    }

    static NoClassDefFoundError toError(final ClassNotFoundException e) {
        final NoClassDefFoundError error = new NoClassDefFoundError(e.getMessage());
        error.setStackTrace(e.getStackTrace());
        return error;
    }

    static ExecutionHandler createInstance(final Constructor<? extends ExecutionHandler> handlerCtor) {
        try {
            return handlerCtor.newInstance();
        } catch (InstantiationException e) {
            throw toError(e);
        } catch (IllegalAccessException e) {
            throw toError(e);
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error ee) {
                throw ee;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
    }

    static class SuppFunc implements Function<ExecutionContext, ExecutionHandler>, Serializable {
        private static final long serialVersionUID = -2945266846035910692L;

        private final Supplier<? extends ExecutionHandler> supplier;

        SuppFunc(final Supplier<? extends ExecutionHandler> supplier) {
            this.supplier = supplier;
        }

        public ExecutionHandler apply(final ExecutionContext ignored) {
            return supplier.get();
        }
    }

    static class CtorSupp implements Supplier<ExecutionHandler>, Serializable {
        private static final long serialVersionUID = 3922300192314041240L;

        private final Constructor<? extends ExecutionHandler> handlerCtor;

        CtorSupp(final Constructor<? extends ExecutionHandler> handlerCtor) {
            this.handlerCtor = handlerCtor;
        }

        public ExecutionHandler get() {
            return createInstance(handlerCtor);
        }

        Object writeReplace() {
            return new Ser(handlerCtor.getDeclaringClass());
        }

        static final class Ser implements Serializable {
            private static final long serialVersionUID = 1696553894340019656L;

            private final Class<? extends ExecutionHandler> clazz;

            Ser(final Class<? extends ExecutionHandler> clazz) {
                this.clazz = clazz;
            }

            Object readResolve() {
                return supplierOf(constructorOf(clazz));
            }
        }
    }

    static class NameSupp implements Supplier<ExecutionHandler>, Serializable {
        private static final long serialVersionUID = 6466027448461014280L;

        private final String className;
        private Class<? extends ExecutionHandler> clazz;

        NameSupp(final String className) {
            this.className = className;
        }

        public ExecutionHandler get() {
            Class<? extends ExecutionHandler> clazz = this.clazz;
            if (clazz == null) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = NameSupp.class.getClassLoader();
                }
                try {
                    this.clazz = clazz = cl.loadClass(className)
                            .asSubclass(ExecutionHandler.class);
                } catch (ClassNotFoundException e) {
                    final NoClassDefFoundError error = new NoClassDefFoundError(e.getMessage() + " (class loader " + cl + ")");
                    error.setStackTrace(e.getStackTrace());
                    error.initCause(e.getCause());
                    throw error;
                }
            }
            return createInstance(constructorOf(clazz));
        }

        Object writeReplace() throws InvalidClassException {
            final Class<? extends ExecutionHandler> clazz = this.clazz;
            return clazz == null ? this : new CtorSupp.Ser(clazz);
        }
    }
}
