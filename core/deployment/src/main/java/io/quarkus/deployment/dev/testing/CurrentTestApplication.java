package io.quarkus.deployment.dev.testing;

import java.util.function.Consumer;

import io.quarkus.bootstrap.app.CuratedApplication;

/**
 * This class is a bit of a hack, it provides a way to pass in the current curratedApplication into the TestExtension
 */
public class CurrentTestApplication implements Consumer<CuratedApplication> {
    public static volatile CuratedApplication curatedApplication;

    @Override
    public void accept(CuratedApplication c) {
        curatedApplication = c;
    }
}
