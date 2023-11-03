package io.quarkus.test.component.beans;

import jakarta.enterprise.context.Dependent;

@Dependent
public class Delta {

    public boolean ping() {
        return true;
    }

    public void onBoolean() {
    }

}
