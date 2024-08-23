package org.acme.lib;

import jakarta.inject.Singleton;

@Singleton
public class LibService {
    public String bar() {
        return "bar";
    }
}
