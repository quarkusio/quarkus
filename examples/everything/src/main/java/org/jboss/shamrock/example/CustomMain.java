package org.jboss.shamrock.example;

import org.jboss.shamrock.runtime.Shamrock;

public class CustomMain {

    public static void main(String... args) throws Exception {
        System.out.println("Using a custom main method");
        Shamrock.main(args);
    }

}
