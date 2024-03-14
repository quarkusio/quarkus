package io.quarkus.rest.client.reactive.runtime;

import static org.jboss.resteasy.reactive.client.impl.RestClientRequestContext.INVOKED_METHOD_PARAMETERS_PROP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;

import io.quarkus.rest.client.reactive.ComputedParamContext;

@SuppressWarnings("unused")
public class ComputedParamContextImpl implements ComputedParamContext {

    private final String name;
    private final List<MethodParameter> parameters;

    public ComputedParamContextImpl(String name, ClientRequestContext context) {
        this.name = name;
        this.parameters = createParameters(context);
    }

    @SuppressWarnings("unchecked")
    private static List<MethodParameter> createParameters(ClientRequestContext context) {
        Object property = context.getProperty(INVOKED_METHOD_PARAMETERS_PROP);
        if (property == null) {
            throw new IllegalStateException(
                    "property " + INVOKED_METHOD_PARAMETERS_PROP + " should have been part of the client context");
        }
        List<Object> methodParameterValues = (List<Object>) property;
        if (methodParameterValues.isEmpty()) {
            return Collections.emptyList();
        }
        List<MethodParameter> result = new ArrayList<>(methodParameterValues.size());
        for (Object value : methodParameterValues) {
            result.add(new MethodParameterImpl(value));
        }
        return result;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<MethodParameter> methodParameters() {
        return parameters;
    }

    public static Object getMethodParameterFromContext(ClientRequestContext context, int index) {
        Object property = context.getProperty(INVOKED_METHOD_PARAMETERS_PROP);
        if (property == null) {
            throw new IllegalStateException(
                    "property " + INVOKED_METHOD_PARAMETERS_PROP + " should have been part of the client context");
        }
        List<Object> methodParameterValues = (List<Object>) property;
        if (index > methodParameterValues.size() - 1) {
            throw new IllegalArgumentException("Invalid parameter index '" + index + "' used when obtaining parameter values");
        }
        return methodParameterValues.get(index);
    }

    private static class MethodParameterImpl implements MethodParameter {

        private final Object value;

        private MethodParameterImpl(Object value) {
            this.value = value;
        }

        @Override
        public Object value() {
            return value;
        }
    }
}
