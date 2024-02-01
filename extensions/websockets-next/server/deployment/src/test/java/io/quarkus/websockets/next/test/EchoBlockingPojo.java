package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.Context;

@WebSocket("/echo-blocking-pojo")
public class EchoBlockingPojo {

    @Inject
    EchoService echoService;

    @OnMessage
    Message echo(Message msg) {
        assertTrue(Context.isOnWorkerThread());
        Message ret = new Message();
        ret.setMsg(echoService.echo(msg.getMsg()));
        return ret;
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
