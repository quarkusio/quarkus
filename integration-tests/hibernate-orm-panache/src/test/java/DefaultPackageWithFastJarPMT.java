import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.it.panache.TestResources;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class DefaultPackageWithFastJarPMT {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PackagelessCat.class, TestResources.class))
            .setApplicationName("default-package")
            .setApplicationVersion(Version.getVersion())
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.package.type", "fast-jar");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testJarCreated() {
        assertThat(prodModeTestResults.getResults()).hasSize(1);
        assertThat(prodModeTestResults.getResults().get(0).getPath()).exists();
    }
}
