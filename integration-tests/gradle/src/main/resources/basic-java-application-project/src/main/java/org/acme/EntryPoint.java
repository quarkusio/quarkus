package org.acme;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class EntryPoint {
    public static void main(String[] args) {
        Quarkus.run();
    }
}
