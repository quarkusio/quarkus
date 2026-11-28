package io.quarkus.removedclasses;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class RemovedResourceBuildItemLegacyJarTest extends AbstractRemovedResourceBuildItemTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = application("legacy-jar");
}
