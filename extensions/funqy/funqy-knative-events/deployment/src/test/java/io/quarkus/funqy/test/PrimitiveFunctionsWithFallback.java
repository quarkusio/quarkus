package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class PrimitiveFunctionsWithFallback {
    @Funq
    public String toLowerCase(String val) {
        return val.toLowerCase();
    }

    @Funq
    public int doubleIt(int val) {
        return val * 2;
    }

    @Funq
    public void noop() {
    }

    @Funq
    @CloudEventMapping(trigger = "echo", responseType = "echo.output", responseSource = "echo")
    public String annotatedEcho(String echo) {
        return echo;
    }

    @Funq
    @CloudEventMapping(trigger = "*", responseType = "fallback.output", responseSource = "echo")
    public String annotatedFallback(String echo) {
        return "Fallback for " + echo;
    }

}
