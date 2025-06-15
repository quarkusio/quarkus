package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.List;

import org.jboss.resteasy.reactive.common.util.DeploymentUtils;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class ArrayConverter implements ParameterConverter {

    private final ParameterConverter delegate;
    private final String elementType;

    public ArrayConverter(ParameterConverter delegate, String elementType) {
        this.delegate = delegate;
        this.elementType = elementType;
    }

    @Override
    public Object convert(Object parameter) {
        Class<?> elementTypeClass = DeploymentUtils.loadClass(elementType);
        if (parameter == null) {
            return Array.newInstance(elementTypeClass, 0);
        }
        if (parameter instanceof List) {
            List<?> parameterAsList = (List<?>) parameter;
            Object result = Array.newInstance(elementTypeClass, parameterAsList.size());
            for (int i = 0; i < parameterAsList.size(); i++) {
                Array.set(result, i,
                        delegate == null ? parameterAsList.get(i) : delegate.convert(parameterAsList.get(i)));
            }
            return result;
        }
        Object result = Array.newInstance(elementTypeClass, 1);
        Array.set(result, 0, parameter);
        return result;
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        if (delegate != null)
            delegate.init(deployment, rawType, genericType, annotations);
    }

    public ParameterConverter getDelegate() {
        return delegate;
    }

    public static class ArraySupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;
        private String elementType;

        public ArraySupplier() {
        }

        // invoked by reflection for BeanParam in ClassInjectorTransformer
        public ArraySupplier(ParameterConverterSupplier delegate, String elementType) {
            this.delegate = delegate;
            this.elementType = elementType;
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new ArrayConverter(null, elementType)
                    : new ArrayConverter(delegate.get(), elementType);
        }

        @Override
        public String getClassName() {
            return ArrayConverter.class.getName();
        }

        @Override
        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        public ArraySupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }

        public String getElementType() {
            return elementType;
        }

        public void setElementType(String elementType) {
            this.elementType = elementType;
        }
    }
}
