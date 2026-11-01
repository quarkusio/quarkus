package io.quarkus.removedclasses;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class RemovedResourceBuildItemFastJarTest extends AbstractRemovedResourceBuildItemTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = application("fast-jar");
}
