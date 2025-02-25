package io.quarkus.qute.deployment.devmode;

import io.quarkus.qute.TemplateGlobal;

@TemplateGlobal
public class TestGlobals {

    public static String testFoo() {
        return "baz";
    }
}
