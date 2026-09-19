package io.quarkus.websockets.next.test.devmode;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/greeting")
public class Greeting {

    @OnTextMessage
    String greet(String name) {
        return "Hello " + name;
    }
}
