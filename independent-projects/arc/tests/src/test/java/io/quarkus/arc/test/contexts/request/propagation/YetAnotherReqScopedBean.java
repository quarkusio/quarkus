package io.quarkus.arc.test.contexts.request.propagation;

import java.util.Random;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class YetAnotherReqScopedBean {

    int randomNumber;

    public YetAnotherReqScopedBean() {
        this.randomNumber = new Random().nextInt();
    }

    public int getRandomNumber() {
        return randomNumber;
    }
}
