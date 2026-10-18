package io.quarkus.websockets.next.test;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/echo-multi-bidi")
public class EchoMultiBidi {

    @OnTextMessage
    Multi<String> echo(Multi<String> multi) {
        return multi;
    }

}
