package io.quarkus.it.mockbean;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

@Singleton
public class MyContainerConsumer {

    private final MyContainer<String> stringContainer;
    private final MyContainer<Integer> integerContainer;

    public MyContainerConsumer(MyContainer<String> stringContainer, MyContainer<Integer> integerContainer) {
        this.stringContainer = stringContainer;
        this.integerContainer = integerContainer;
    }

    public String createString() {
        List<String> strings = new ArrayList<>(integerContainer.getValue());
        for (int i = 0; i < integerContainer.getValue(); i++) {
            strings.add(stringContainer.getValue());
        }
        return String.join(" ", strings);
    }
}
