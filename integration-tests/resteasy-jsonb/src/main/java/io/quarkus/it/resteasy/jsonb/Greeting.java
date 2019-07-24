package io.quarkus.it.resteasy.jsonb;

// the fact that there are 2 implementation of Greeter (which is used as the return type in the JAX-RS resource)
// ensures that no serializer is generated
public class Greeting implements Greeter {

    private final String message;

    public Greeting(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void sayHello() {

    }
}
