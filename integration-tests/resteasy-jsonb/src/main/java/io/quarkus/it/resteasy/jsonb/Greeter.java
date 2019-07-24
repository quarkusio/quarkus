package io.quarkus.it.resteasy.jsonb;

public interface Greeter {

    void sayHello();

    static class Default implements Greeter {
        @Override
        public void sayHello() {

        }
    }

}
