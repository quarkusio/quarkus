package org.acme.lib;

import javax.inject.Singleton;

@Singleton
public class LibService {
    public String bar() {
        return "bar";
    }
}
