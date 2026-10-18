package io.quarkus.websockets;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;

public class ChatMessageDecoder implements Decoder.Text<ChatMessageDTO> {

    private final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public ChatMessageDTO decode(String string) throws DecodeException {
        return jsonb.fromJson(
                string,
                ChatMessageDTO.class);
    }

    @Override
    public boolean willDecode(String string) {
        return Boolean.TRUE;
    }

}