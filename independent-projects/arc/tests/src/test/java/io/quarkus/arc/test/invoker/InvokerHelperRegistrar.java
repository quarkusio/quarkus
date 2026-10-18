package io.quarkus.arc.test.invoker;

import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.InvokerFactory;
import io.quarkus.arc.processor.InvokerInfo;

public class InvokerHelperRegistrar implements BeanRegistrar {
    private final Class<?> beanClass;
    private final InvokerHelperAction action;

    public InvokerHelperRegistrar(Class<?> beanClass, InvokerHelperAction action) {
        this.beanClass = beanClass;
        this.action = action;
    }

    @Override
    public void register(RegistrationContext context) {
        Map<String, InvokerInfo> map = new LinkedHashMap<>();
        BeanInfo bean = context.beans().withBeanClass(beanClass).firstResult().orElseThrow();
        action.accept(bean, context.getInvokerFactory(), map);
        synthesizeInvokerHelper(context, map);
    }

    public static void synthesizeInvokerHelper(RegistrationContext context, Map<String, InvokerInfo> invokers) {
        context.configure(InvokerHelper.class)
                .types(InvokerHelper.class)
                .creator(InvokerHelperCreator.class)
                .param("names", invokers.keySet().toArray(String[]::new))
                .param("invokers", invokers.values().toArray(InvokerInfo[]::new))
                .done();
    }

    @FunctionalInterface
    public interface InvokerHelperAction {
        void accept(BeanInfo bean, InvokerFactory invokerFactory, Map<String, InvokerInfo> map);
    }
}
