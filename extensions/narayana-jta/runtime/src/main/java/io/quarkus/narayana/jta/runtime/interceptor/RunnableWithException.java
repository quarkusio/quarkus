package io.quarkus.narayana.jta.runtime.interceptor;

@FunctionalInterface
public interface RunnableWithException {
    void run() throws Exception;
}
