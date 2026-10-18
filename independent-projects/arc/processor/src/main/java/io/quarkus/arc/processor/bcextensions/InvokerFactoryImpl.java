package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.InvokerFactory;
import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.invoke.InvokerBuilder;
import jakarta.enterprise.lang.model.declarations.MethodInfo;

class InvokerFactoryImpl implements InvokerFactory {
    private final io.quarkus.arc.processor.InvokerFactory arcInvokerFactory;

    InvokerFactoryImpl(io.quarkus.arc.processor.InvokerFactory arcInvokerFactory) {
        this.arcInvokerFactory = arcInvokerFactory;
    }

    @Override
    public InvokerBuilder<InvokerInfo> createInvoker(BeanInfo bean, MethodInfo method) {
        return new InvokerBuilderImpl(arcInvokerFactory.createInvoker(
                ((BeanInfoImpl) bean).arcBeanInfo, ((MethodInfoImpl) method).jandexDeclaration));
    }
}
