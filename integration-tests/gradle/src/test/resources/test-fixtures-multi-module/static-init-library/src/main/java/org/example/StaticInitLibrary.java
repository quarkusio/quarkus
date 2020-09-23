package org.example;

public class StaticInitLibrary {

    private static ObjectFactory factory;

    public static void init(ObjectFactory f) {
        factory = f;
    }

}
