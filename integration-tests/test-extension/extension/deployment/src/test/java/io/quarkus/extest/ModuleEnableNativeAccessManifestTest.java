package io.quarkus.extest;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies the Enable-Native-Access manifest entry for uber-jar packaging.
 */
public class ModuleEnableNativeAccessManifestTest extends AbstractModuleEnableNativeAccessManifestTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.package.jar.type", "uber-jar");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Override
    ProdModeTestResults prodModeTestResults() {
        return prodModeTestResults;
    }
}
