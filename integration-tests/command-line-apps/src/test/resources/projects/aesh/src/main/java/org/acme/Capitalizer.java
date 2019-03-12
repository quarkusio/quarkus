package org.acme;

import javax.inject.Singleton;


@Singleton
public class Capitalizer {

    public String perform(String input) {
        return input.toUpperCase();
    }

}
