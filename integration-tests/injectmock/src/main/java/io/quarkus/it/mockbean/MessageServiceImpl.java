package io.quarkus.it.mockbean;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessageServiceImpl implements MessageService {

    @Override
    public String getMessage() {
        return "hello";
    }
}
