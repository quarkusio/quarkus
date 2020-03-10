package io.quarkus.funqy.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionInvoker {
    protected String name;
    protected Class targetClass;
    protected Method method;
    protected FunctionConstructor constructor;
    protected ArrayList<ValueInjector> parameterInjectors;
    protected Class inputType;
    protected Class outputType;
    protected Map<String, Object> bindingContext = new ConcurrentHashMap<>();

    public FunctionInvoker(String name, Class targetClass, Method method) {
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
        if (method.getReturnType() != null) {
            outputType = method.getReturnType();
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

    public boolean hasOutput() {
        return outputType != null;
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
            response.setOutput(result);
        } catch (IllegalAccessException e) {
            throw new InternalError("Failed to invoke function", e);
        } catch (InvocationTargetException e) {
            throw new ApplicationException(e.getCause());
        }
    }
}
