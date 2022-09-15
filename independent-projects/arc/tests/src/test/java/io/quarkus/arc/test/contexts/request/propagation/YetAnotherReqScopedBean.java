package io.quarkus.arc.test.contexts.request.propagation;

import jakarta.enterprise.context.RequestScoped;
import java.util.Random;

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
