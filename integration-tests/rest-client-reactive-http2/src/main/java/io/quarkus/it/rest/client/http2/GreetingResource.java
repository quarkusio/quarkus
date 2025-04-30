package io.quarkus.it.rest.client.http2;

public class GreetingResource extends AbstractGreetingResource {
    @Override
    public String hello() {
        return "Hello";
    }
}
