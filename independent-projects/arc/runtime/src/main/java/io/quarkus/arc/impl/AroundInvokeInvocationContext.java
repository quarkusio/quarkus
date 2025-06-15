package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.arc.ArcInvocationContext;

/**
 * An {@link jakarta.interceptor.InvocationContext} for {@link jakarta.interceptor.AroundInvoke} interceptors.
 * <p>
 * A new instance is created for the first interceptor in the chain. Furthermore, subsequent interceptors receive a new instance
 * of {@link NextAroundInvokeInvocationContext}. This does not comply with the spec but allows for "asynchronous continuation"
 * of an interceptor chain execution. In other words, it is possible to "cut off" the chain (interceptors executed before
 * dispatch return immediately) and execute all remaining interceptors asynchronously, possibly on a different thread.
 * <p>
 * Note that context data and method parameters are mutable and are not guarded/synchronized. We expect them to be modified
 * before or after dispatch. If modified before and after dispatch an unpredictable behavior may occur.
 */
class AroundInvokeInvocationContext extends AbstractInvocationContext {

    static Object perform(Object target, Object[] args, InterceptedMethodMetadata metadata) throws Exception {
        if (metadata.chain.isEmpty()) {
            return metadata.aroundInvokeForward.apply(target, new AroundInvokeInvocationContext(target, args, metadata));
        }
        return metadata.chain.get(0).invoke(new AroundInvokeInvocationContext(target, args, metadata));
    }

    private final InterceptedMethodMetadata metadata;

    AroundInvokeInvocationContext(Object target, Object[] args, InterceptedMethodMetadata metadata) {
        super(target, args, new ContextDataMap(metadata.bindings));
        this.metadata = metadata;
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return metadata.bindings;
    }

    @Override
    public Method getMethod() {
        return metadata.method;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Object[] params) {
        validateParameters(metadata.method, params);
        this.parameters = params;
    }

    @Override
    public Object proceed() throws Exception {
        return proceed(1);
    }

    private Object proceed(int currentPosition) throws Exception {
        try {
            if (currentPosition < metadata.chain.size()) {
                // Invoke the next interceptor in the chain
                return metadata.chain.get(currentPosition)
                        .invoke(new NextAroundInvokeInvocationContext(currentPosition + 1));
            } else {
                // Invoke the target method
                return metadata.aroundInvokeForward.apply(target, this);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    class NextAroundInvokeInvocationContext implements ArcInvocationContext {

        private final int position;

        public NextAroundInvokeInvocationContext(int position) {
            this.position = position;
        }

        @Override
        public Object proceed() throws Exception {
            return AroundInvokeInvocationContext.this.proceed(position);
        }

        @Override
        public Object getTarget() {
            return AroundInvokeInvocationContext.this.getTarget();
        }

        @Override
        public Object getTimer() {
            return AroundInvokeInvocationContext.this.getTimer();
        }

        @Override
        public Method getMethod() {
            return AroundInvokeInvocationContext.this.getMethod();
        }

        @Override
        public Constructor<?> getConstructor() {
            return AroundInvokeInvocationContext.this.getConstructor();
        }

        @Override
        public Object[] getParameters() {
            return AroundInvokeInvocationContext.this.getParameters();
        }

        @Override
        public void setParameters(Object[] params) {
            AroundInvokeInvocationContext.this.setParameters(params);
        }

        @Override
        public Map<String, Object> getContextData() {
            return AroundInvokeInvocationContext.this.getContextData();
        }

        @Override
        public Set<Annotation> getInterceptorBindings() {
            return AroundInvokeInvocationContext.this.getInterceptorBindings();
        }

        @Override
        public <T extends Annotation> T findIterceptorBinding(Class<T> annotationType) {
            return AroundInvokeInvocationContext.this.findIterceptorBinding(annotationType);
        }

        @Override
        public <T extends Annotation> List<T> findIterceptorBindings(Class<T> annotationType) {
            return AroundInvokeInvocationContext.this.findIterceptorBindings(annotationType);
        }

    }

}
