package io.quarkus.camel.it.core;

import org.apache.camel.builder.RouteBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CamelNetty4HttpRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("netty4-http:http://0.0.0.0:8999/test").process(e -> {
            String request = e.getIn().getBody(String.class);
            String reply = "Hello " + request;

            e.getIn().setBody(reply);
        });
    }
}
