package io.quarkus.test.junit;

import java.util.function.Consumer;

import io.smallrye.mutiny.Uni;

// TODO: present the test with an interface
public class UniResult<T> {

    private Uni<T> uni;
    private Consumer<T> itemAssertion;

    public void assertItem(Uni<T> uni, Consumer<T> itemAssertion) {
        this.uni = uni;
        this.itemAssertion = itemAssertion;
    }

    public Uni<T> getUni() {
        return uni;
    }

    public Consumer<T> getItemAssertion() {
        return itemAssertion;
    }
}
