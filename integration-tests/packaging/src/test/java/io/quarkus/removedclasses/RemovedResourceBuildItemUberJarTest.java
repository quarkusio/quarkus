package io.quarkus.removedclasses;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class RemovedResourceBuildItemUberJarTest extends AbstractRemovedResourceBuildItemTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = application("uber-jar");
}
