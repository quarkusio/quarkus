package io.quarkus.websockets.next.test.codec;

import java.util.List;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket("/find")
public class Find extends AbstractFind {

    @OnMessage
    Item find(List<Item> items) {
        return super.find(items);
    }

}
