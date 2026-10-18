package io.quarkus.grpc.client.interceptors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Calls {

    static final List<String> LIST = new CopyOnWriteArrayList<>();

    static void add(Class<?> interceptorClass) {
        LIST.add(interceptorClass.getName());
    }

}
