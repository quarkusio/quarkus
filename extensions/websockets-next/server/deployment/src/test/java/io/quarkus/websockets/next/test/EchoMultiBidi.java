package io.quarkus.websockets.next.test;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/echo-multi-bidi")
public class EchoMultiBidi {

    @OnMessage
    Multi<String> echo(Multi<String> multi) {
        return multi;
    }

}
