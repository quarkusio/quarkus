package io.quarkus.reactivemessaging.http;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.vertx.core.buffer.Buffer;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 28/08/2019
 */
public class HttpMessage implements Message<Buffer> {

    private final Buffer buffer;

    public HttpMessage(Buffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public Buffer getPayload() {
        return buffer;
    }
}
