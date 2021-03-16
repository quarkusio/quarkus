package io.quarkus.bootstrap.classloader;

public class InterruptClass implements Runnable {

    @Override
    public void run() {
        try {
            Thread.currentThread().interrupt();
            Class.forName("io.quarkus.bootstrap.classloader.ClassToLoad");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
