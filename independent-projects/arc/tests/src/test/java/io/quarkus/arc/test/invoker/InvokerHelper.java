package io.quarkus.arc.test.invoker;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import jakarta.enterprise.invoke.Invoker;

public class InvokerHelper {
    private final Map<String, Invoker<?, ?>> invokers;

    public InvokerHelper(Map<String, Invoker<?, ?>> invokers) {
        this.invokers = invokers;
    }

    public <T, R> Invoker<T, R> getInvoker(String id) {
        Invoker<T, R> result = (Invoker<T, R>) invokers.get(id);
        assertNotNull(result);
        return result;
    }
}
