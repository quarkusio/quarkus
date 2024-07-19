package io.quarkus.deployment.dev.testing;

import java.util.function.Consumer;

import io.quarkus.bootstrap.app.CuratedApplication;

/**
 * This class is a bit of a hack, it provides a way to pass in the current curratedApplication into the TestExtension
 * TODO It is only needed for QuarkusMainTest, so we may be able to find a better way.
 * For example, what about JUnit state?
 */
public class CurrentTestApplication implements Consumer<CuratedApplication> {
    public static volatile CuratedApplication curatedApplication;

    @Override
    public void accept(CuratedApplication c) {
        curatedApplication = c;
    }
}
