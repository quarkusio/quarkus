package io.quarkus.it.spring;

import org.springframework.beans.factory.annotation.Value;

@PrototypeService
public class MessageProducer {

    @Value("${greeting.message}")
    String message;

    public String getPrefix() {
        return message;
    }
}
