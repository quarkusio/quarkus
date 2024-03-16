package io.quarkus.websockets.next.test.sessioncontext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;

@SessionScoped
public class SessionScopedBean {

    static final AtomicBoolean DESTROYED = new AtomicBoolean();

    private final AtomicReference<String> lastMessage = new AtomicReference<>("");

    public String appendAndGet(String message) {
        return lastMessage.accumulateAndGet(message, (s1, s2) -> s1 + s2);
    }

    @PreDestroy
    void destroy() {
        DESTROYED.set(true);
    }

}
