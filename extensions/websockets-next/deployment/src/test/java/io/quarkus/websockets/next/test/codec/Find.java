package io.quarkus.websockets.next.test.codec;

import java.util.List;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/find")
public class Find extends AbstractFind {

    @OnTextMessage
    Item find(List<Item> items) {
        return super.find(items);
    }

}
