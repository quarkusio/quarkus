package io.quarkus.websockets;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

public class ChatMessageEncoder implements Encoder.Text<ChatMessageDTO> {

    private final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public String encode(ChatMessageDTO t) throws EncodeException {
        return jsonb.toJson(t);
    }

}