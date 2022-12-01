package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import io.netty.buffer.ByteBuf;

public interface VertxOutput {
    void write(ByteBuf data, boolean last) throws IOException;

    CompletionStage<Void> writeNonBlocking(ByteBuf data, boolean last);
}
