package io.quarkus.removedclasses;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class RemovedResourceFastJarTest extends AbstractRemovedResourceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = application("fast-jar");
}
