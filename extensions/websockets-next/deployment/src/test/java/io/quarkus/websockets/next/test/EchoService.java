package io.quarkus.websockets.next.test;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EchoService {

    public String echo(String msg) {
        return msg;
    }
}
