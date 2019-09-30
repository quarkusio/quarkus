package io.quarkus.it.mockbean;

import javax.inject.Singleton;

@Singleton
public class MessageService {

    public String getMessage() {
        return "hello";
    }
}
