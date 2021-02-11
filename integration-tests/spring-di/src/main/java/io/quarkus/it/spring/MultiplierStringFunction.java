package io.quarkus.it.spring;

import org.springframework.stereotype.Component;

@Component("multiply")
public class MultiplierStringFunction implements StringFunction {

    @Override
    public String apply(String input) {
        return String.format("%s%s", input, input);
    }
}
