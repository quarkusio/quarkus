package io.quarkus.websockets.test;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class EchoService {

    public String echo(String msg) {
        return msg;
    }
}
