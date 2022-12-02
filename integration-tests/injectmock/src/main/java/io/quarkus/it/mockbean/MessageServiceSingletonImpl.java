package io.quarkus.it.mockbean;

import javax.inject.Singleton;

@Singleton
public class MessageServiceSingletonImpl implements MessageServiceSingleton {

    @Override
    public String getMessage() {
        return "hello";
    }
}
