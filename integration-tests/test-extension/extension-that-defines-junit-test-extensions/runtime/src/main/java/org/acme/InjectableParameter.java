package org.acme;

import java.math.BigInteger;
import java.util.concurrent.CopyOnWriteArrayList;

public class InjectableParameter {
    // Add some private classes to exercise serialization
    private BigInteger integer;

    CopyOnWriteArrayList concurrent;

    InjectableParameter() {
        integer = new BigInteger(String.valueOf(Math.round(Math.random() * 100000)));
        concurrent = new CopyOnWriteArrayList<>();
        concurrent.add(integer);
    }

}
