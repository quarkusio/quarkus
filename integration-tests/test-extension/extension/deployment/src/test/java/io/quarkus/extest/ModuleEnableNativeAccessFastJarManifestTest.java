package io.quarkus.extest;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies the Enable-Native-Access manifest entry for fast-jar packaging.
 */
public class ModuleEnableNativeAccessFastJarManifestTest extends AbstractModuleEnableNativeAccessManifestTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.package.jar.type", "fast-jar");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Override
    ProdModeTestResults prodModeTestResults() {
        return prodModeTestResults;
    }
}
