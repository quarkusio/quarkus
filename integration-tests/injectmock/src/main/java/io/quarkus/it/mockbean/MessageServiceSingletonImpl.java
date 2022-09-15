package io.quarkus.it.mockbean;

import jakarta.inject.Singleton;

@Singleton
public class MessageServiceSingletonImpl implements MessageServiceSingleton {

    @Override
    public String getMessage() {
        return "hello";
    }
}
