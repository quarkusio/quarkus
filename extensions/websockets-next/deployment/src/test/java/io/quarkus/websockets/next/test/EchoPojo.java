package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@WebSocket(path = "/echo-pojo")
public class EchoPojo {

    @Inject
    EchoService echoService;

    @OnTextMessage
    Uni<Message> echo(Message msg) {
        assertTrue(Context.isOnEventLoopThread());
        Message ret = new Message();
        ret.setMsg(echoService.echo(msg.getMsg()));
        return Uni.createFrom().item(ret);
    }

    public static class Message {

        private String msg;

        public String getMsg() {
            return msg;
        }

        public void setMsg(String value) {
            this.msg = value;
        }

    }

}
