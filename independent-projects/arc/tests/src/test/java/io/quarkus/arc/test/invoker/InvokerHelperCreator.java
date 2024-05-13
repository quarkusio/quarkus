package io.quarkus.arc.test.invoker;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.invoke.Invoker;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

class InvokerHelperCreator implements BeanCreator<InvokerHelper> {
    @Override
    public InvokerHelper create(SyntheticCreationalContext<InvokerHelper> context) {
        String[] names = (String[]) context.getParams().get("names");
        Invoker<?, ?>[] invokers = (Invoker<?, ?>[]) context.getParams().get("invokers");
        Map<String, Invoker<?, ?>> map = new HashMap<>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], invokers[i]);
        }
        return new InvokerHelper(map);
    }
}
