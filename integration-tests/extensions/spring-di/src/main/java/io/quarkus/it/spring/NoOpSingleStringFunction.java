package io.quarkus.it.spring;

import org.springframework.stereotype.Component;

@Component("noop")
public class NoOpSingleStringFunction implements StringFunction {

    @Override
    public String apply(String s) {
        return s;
    }
}
