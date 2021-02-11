package io.quarkus.changeagent;

import java.lang.instrument.Instrumentation;

public class ClassChangeAgent {

    private static volatile Instrumentation instrumentation;

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public static void premain(java.lang.String s, Instrumentation i) {
        instrumentation = i;

    }

}
