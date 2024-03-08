import io.quarkus.builder.Version
import io.quarkus.test.ProdBuildResults
import io.quarkus.test.ProdModeTestResults
import io.quarkus.test.QuarkusProdModeTest
import org.assertj.core.api.Assertions.assertThat
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class DefaultPackageWithFastJarPMT {

    @ProdBuildResults private var prodModeTestResults: ProdModeTestResults? = null

    @Test
    fun testJarCreated() {
        assertThat(prodModeTestResults?.results).hasSize(1)
        assertThat(prodModeTestResults?.results?.get(0)?.path).exists()
    }

    companion object {
        @RegisterExtension
        @JvmField
        val config =
            QuarkusProdModeTest()
                .withApplicationRoot { jar: JavaArchive ->
                    jar.addClasses(PackagelessCat::class.java)
                }
                .setApplicationName("default-package")
                .setApplicationVersion(Version.getVersion())
                .withConfigurationResource("application.properties")
                .overrideConfigKey("quarkus.package.jar.type", "fast-jar")
    }
}
