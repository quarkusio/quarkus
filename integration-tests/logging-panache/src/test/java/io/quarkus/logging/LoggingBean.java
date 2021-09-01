package io.quarkus.logging;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

@Singleton
public class LoggingBean {
    // not final to prevent constant inlining
    private static String msg = "Heya!";

    static {
        Log.info(msg);
    }

    @PostConstruct
    public void setup() {
        Log.tracef("%s created", LoggingBean.class.getSimpleName());
    }

    public void doSomething() {
        if (Log.isDebugEnabled()) {
            Log.debug("starting massive computation");
        }

        Log.debugf("one: %d", 42);
        Log.tracef("two: %d | %d", 42, 13);
        Log.debugf("three: %d | %d | %d", 42, 13, 1);

        Log.debugv("one: {0}", "foo");
        Log.infov("two: {0} | {1}", "foo", "bar");
        Log.warnv("three: {0} | {1} | {2}", "foo", "bar", "baz");
        Log.errorv("four: {0} | {1} | {2} | {3}", "foo", "bar", "baz", "quux");

        Exception error = new NoStackTraceTestException();

        Log.warnv(error, "{0} | {1} | {2} | {3}", "foo", "bar", "baz", "quux");

        Log.error("Hello Error", error);
    }

}
