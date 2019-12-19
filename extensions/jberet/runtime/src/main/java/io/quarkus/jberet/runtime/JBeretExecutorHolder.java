package io.quarkus.jberet.runtime;

import java.util.concurrent.ExecutorService;

class JBeretExecutorHolder {

    static volatile ExecutorService executor;

    static void set(ExecutorService executor) {
        JBeretExecutorHolder.executor = executor;
    }

    static ExecutorService get() {
        return executor;
    }

}
