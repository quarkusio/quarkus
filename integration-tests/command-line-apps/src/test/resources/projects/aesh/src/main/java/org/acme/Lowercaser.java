package org.acme;

import javax.inject.Singleton;


@Singleton
public class Lowercaser {

    public String perform(String input) {
        return input.toLowerCase();
    }

}
