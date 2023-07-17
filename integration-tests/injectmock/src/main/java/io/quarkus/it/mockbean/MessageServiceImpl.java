package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessageServiceImpl implements MessageService {

    @Override
    public String getMessage() {
        return "hello";
    }
}
