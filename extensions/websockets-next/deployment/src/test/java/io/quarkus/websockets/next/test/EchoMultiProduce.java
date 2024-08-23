package io.quarkus.websockets.next.test;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/echo-multi-produce")
public class EchoMultiProduce {

    @OnTextMessage
    Multi<String> echo(String msg) {
        return Multi.createFrom().item(msg);
    }

}
