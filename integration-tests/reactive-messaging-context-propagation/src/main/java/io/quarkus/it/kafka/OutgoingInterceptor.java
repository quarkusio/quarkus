package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Identifier;

@ApplicationScoped
@Identifier("flowers-out")
public class OutgoingInterceptor implements io.smallrye.reactive.messaging.OutgoingInterceptor {

    @Inject
    RequestBean reqBean;

    @Override
    public Message<?> beforeMessageSend(Message<?> message) {
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        return message;
    }

    @Override
    public void onMessageAck(Message<?> message) {
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
    }

    @Override
    public void onMessageNack(Message<?> message, Throwable failure) {

    }
}
