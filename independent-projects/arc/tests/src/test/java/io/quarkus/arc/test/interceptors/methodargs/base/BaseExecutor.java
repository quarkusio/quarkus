package io.quarkus.arc.test.interceptors.methodargs.base;

public class BaseExecutor {

    public String run() {
        return "run";
    }

    protected void runWorker(Worker run) {

    }

    static class Worker {

    }
}
