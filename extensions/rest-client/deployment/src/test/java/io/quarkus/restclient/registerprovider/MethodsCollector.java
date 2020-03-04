package io.quarkus.restclient.registerprovider;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Singleton;

@Singleton
public class MethodsCollector {

    private final List<String> methods = new CopyOnWriteArrayList<>();

    void collect(String method) {
        methods.add(method);
    }

    List<String> getMethods() {
        return methods;
    }

}
