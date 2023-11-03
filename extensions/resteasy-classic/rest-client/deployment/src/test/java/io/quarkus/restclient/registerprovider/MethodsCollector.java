package io.quarkus.restclient.registerprovider;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Singleton;

@Singleton
public class MethodsCollector {

    private final List<String> methods = new CopyOnWriteArrayList<>();
    private int requestBeanFromFilter;

    void collect(String method) {
        methods.add(method);
    }

    List<String> getMethods() {
        return methods;
    }

    public void setRequestBeanFromFilter(int uniqueNumber) {
        this.requestBeanFromFilter = uniqueNumber;
    }

    public int getRequestBeanFromFilter() {
        return requestBeanFromFilter;
    }

}
