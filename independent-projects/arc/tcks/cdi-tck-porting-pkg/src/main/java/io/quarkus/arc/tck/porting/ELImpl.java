package io.quarkus.arc.tck.porting;

import jakarta.el.ELContext;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.cdi.tck.spi.EL;

public class ELImpl implements EL {
    @Override
    public <T> T evaluateValueExpression(BeanManager beanManager, String expression, Class<T> expectedType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T evaluateMethodExpression(BeanManager beanManager, String expression, Class<T> expectedType,
            Class<?>[] expectedParamTypes, Object[] expectedParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ELContext createELContext(BeanManager beanManager) {
        throw new UnsupportedOperationException();
    }
}
