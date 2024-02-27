package io.quarkus.websockets.next.test;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/echo-multi-produce")
public class EchoMultiProduce {

    @OnMessage
    Multi<String> echo(String msg) {
        return Multi.createFrom().item(msg);
    }

}
