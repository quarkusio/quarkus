package io.quarkus.qute.generator;

import java.util.Arrays;

public class NamespaceExtensionMethods {

    static String ping(Number a, Number b) {
        return "Number, Number";
    }

    static String ping(String a, String b) {
        return "String, String";
    }

    static String ping(String name, String a, Number b) {
        return "String, Number:" + name;
    }

    static String pong(String... values) {
        return "String..." + Arrays.toString(values);
    }

    static String[] pongs() {
        return new String[] { "foo", "bar" };
    }

    static String pingRegex(String name, String a) {
        return "String:" + name + a;
    }

}
