package io.quarkus.funqy.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionInvoker {
    protected String name;
    protected Class<?> targetClass;
    protected Method method;
    protected FunctionConstructor<?> constructor;
    protected ArrayList<ValueInjector> parameterInjectors;
    protected Class<?> inputType;
    protected Class<?> outputType;
    protected boolean isAsync;

    protected Map<String, Object> bindingContext = new ConcurrentHashMap<>();

    public FunctionInvoker(String name, Class<?> targetClass, Method method) {
        this.name = name;
        this.targetClass = targetClass;
        this.method = method;
        if (method.getParameterCount() > 0) {
            parameterInjectors = new ArrayList<>(method.getParameterCount());
            for (int i = 0; i < method.getParameterCount(); i++) {
                Type type = method.getGenericParameterTypes()[i];
                Class clz = method.getParameterTypes()[i];
                Annotation[] annotations = method.getParameterAnnotations()[i];
                ValueInjector injector = ParameterInjector.createInjector(type, clz, annotations);
                if (injector instanceof InputValueInjector) {
                    inputType = clz;
                }
                parameterInjectors.add(injector);
            }
        }
        constructor = new FunctionConstructor(targetClass);
        Class<?> returnType = method.getReturnType();
        if (returnType != null) {
            if (CompletionStage.class.isAssignableFrom(returnType)) {
                outputType = null;
                isAsync = true;
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    Type[] actualParams = ((ParameterizedType) genericReturnType).getActualTypeArguments();
                    if (actualParams.length == 1 && actualParams[0] instanceof Class<?>) {
                        outputType = (Class<?>) actualParams[0];
                    }
                }
                if (outputType == null) {
                    throw new IllegalArgumentException(
                            "CompletionStage must be used with type parameter (e.g. CompletionStage<String>).");
                }
            } else {
                outputType = returnType;
            }
        }
    }

    /**
     * Allow storage of binding specific objects that are specific to the function.
     * i.e. json marshallers
     *
     * @return
     */
    public Map<String, Object> getBindingContext() {
        return bindingContext;
    }

    public boolean hasInput() {
        return inputType != null;
    }

    public Class getInputType() {
        return inputType;
    }

    public Class getOutputType() {
        return outputType;
    }

    protected boolean isAsync() {
        return isAsync;
    }

    protected void setAsync(boolean async) {
        isAsync = async;
    }

    public boolean hasOutput() {
        return outputType != null && !outputType.equals(Void.TYPE);
    }

    public String getName() {
        return name;
    }

    public void invoke(FunqyServerRequest request, FunqyServerResponse response) {
        Object[] args = null;
        if (parameterInjectors != null) {
            args = new Object[parameterInjectors.size()];
            int i = 0;
            for (ValueInjector injector : parameterInjectors) {
                args[i++] = injector.extract(request);
            }
        }
        Object target = constructor.construct();
        try {
            Object result = method.invoke(target, args);
            if (isAsync()) {
                CompletableFuture<Object> withWrappedException = new CompletableFuture<>();
                ((CompletionStage<?>) result).whenCompleteAsync((o, t) -> {
                    if (t != null) {
                        withWrappedException.completeExceptionally(new ApplicationException(t));
                    } else {
                        withWrappedException.complete(o);
                    }
                });
                response.setOutput(withWrappedException);
            } else {
                response.setOutput(CompletableFuture.completedFuture(result));
            }
        } catch (IllegalAccessException e) {
            InternalError ex = new InternalError("Failed to invoke function", e);
            response.setOutput(exceptionalCompletionStage(ex));
            throw ex;
        } catch (InvocationTargetException e) {
            ApplicationException ex = new ApplicationException(e.getCause());
            response.setOutput(exceptionalCompletionStage(ex));
            throw ex;
        } catch (Throwable t) {
            InternalError ex = new InternalError(t);
            response.setOutput(exceptionalCompletionStage(ex));
            throw ex;
        }
    }

    private static CompletionStage<?> exceptionalCompletionStage(Throwable t) {
        CompletableFuture<?> fut = new CompletableFuture<>();
        fut.completeExceptionally(t);
        return fut;
    }
}
